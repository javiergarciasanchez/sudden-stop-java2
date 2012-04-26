/**
 * 
 * This file was automatically generated by the Repast Simphony Agent Editor.
 * Please see http://repast.sourceforge.net/ for details.
 * 
 */

/**
 *
 * Set the package name.
 *
 */
package suddenStop;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static repast.simphony.essentials.RepastEssentials.GetParameter;
import static repast.simphony.essentials.RepastEssentials.GetTickCount;
import static repast.simphony.essentials.RepastEssentials.RemoveAgentFromModel;
import static suddenStop.CashUsage.CASH;
import static suddenStop.CashUsage.LEVERAGE;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import static suddenStop.IndepVarsNames.*;

public class Firm {

	public static SupplyManager supplyManager;
	public static IndependentVarsManager independentVarsManager;

	private FirmState currentState, nextState;
	private boolean toBeKilled = false;
	private ArrayList<Cohort> shadowFirms;

	protected static long agentIDCounter = 1;
	protected long agentIntID = agentIDCounter++;
	protected String agentID = "Firm " + agentIntID;

	public Firm(Context<Object> context) {

		context.add(this);

		nextState = new FirmState();

		nextState.quantityPerPeriod = nextState.getCapital()
				* nextState.getCapitalProductivityPerPeriod();

		currentState = nextState.clone();

		if (!RunEnvironment.getInstance().isBatch()) {
			shadowFirms = new ArrayList<Cohort>(3);
			addToCohorts(context);
		}

	}

	private void addToCohorts(Context<Object> context) {
		Object c = null;
		switch (getCohort(TARGET_LEVERAGE)) {
		case 1:
			c = new Lev1(this);
			context.add(c);
			break;
		case 2:
			c = new Lev2(this);
			context.add(c);
			break;
		case 3:
			c = new Lev3(this);
			context.add(c);
			break;
		}
		shadowFirms.add((Cohort) c);

	}

	public void moveToNextState() {

		// apply innovation
		nextState.firstUnitCost = nextState.firstUnitCost
				/ ((nextState.rDPerPeriod + 1.0) * nextState.rDEfficiency)
				* supplyManager.innovationErrorNormal.nextDouble();

		// Define quantityPerPeriod offered
		nextState.quantityPerPeriod = nextState.getCapital()
				* nextState.getCapitalProductivityPerPeriod();

		nextState.resetExternalEquityAvailable();

		currentState = nextState.clone();

	}

	/**
	 * Estimates if nextDecision would be an exit given nextState and price It
	 * works like ProcessResponseToDemand but without changing the current
	 * situation
	 */
	public boolean estimateResponseToDemand(double price) {

		FirmState tmpSt = nextState.clone();

		return processProfit(tmpSt, price);

	}

	/**
	 * 
	 * Process demand respond and returns false if firm exits the industry,
	 * otherwise returns true
	 * 
	 */
	public void processResponseToDemand(double price) {

		if (!processProfit(currentState, price)) {
			toBeKilled = true;
			return;
		}

		// From here onward all modifications are done in nextState
		nextState = currentState.clone();

		acumulateVariables();

		if (!checkMinCapital(price)) {
			toBeKilled = true;
		}

	}

	public void killShadowFirms() {
		if (RunEnvironment.getInstance().isBatch())
			return;

		for (Cohort c : shadowFirms) {
			RemoveAgentFromModel(c);
		}
	}

	public void planNextYear(double price) {

		raiseFunds(nextState, LEVERAGE, getNetInvestment(currentState, price));

		selectRD();

	}

	/*
	 * Returns true is profit was processed OK, i.e. if it is not an exit
	 */
	private boolean processProfit(FirmState st, double price) {

		double profit = calcProfitPerPeriod(st, price);

		st.profitPerPeriod = profit;

		/*
		 * Funds Generated by Operations (fgo) could be < 0 meaning that
		 * profitPerPeriod is lower than depreciation
		 */
		double fgo = profit + st.getDepreciationPerPeriod();

		// Raise funds to cover negative fgo
		if (fgo < 0) {
			/*
			 * Raise funds will add -fgo to capital, so to keep capital
			 * unaffected by loss -fgo should be subtracted before
			 */

			st.capital -= -fgo;
			return (raiseFunds(st, CASH, -fgo) >= -fgo);
		} else {
			return (calcPerformance(st) >= getMinimumPerformance(st));
		}

	}

	private double getMinimumPerformance(FirmState st) {
		return st.getWACC();
	}

	private double calcProfitPerPeriod(FirmState st, double price) {

		return price * st.getQuantityPerPeriod() - st.getTotVarCostPerPeriod()
				- st.getTotFixedCostPerPeriod() - st.getInterestPerPeriod();

	}

	private double calcPerformance(FirmState st) {
		return (Double) GetParameter("performanceWeight") * st.getPerformance()
				+ (1 - (Double) GetParameter("performanceWeight"))
				* st.getROI();
	}

	private void acumulateVariables() {

		nextState.setPerformance(calcPerformance(currentState));
		nextState.acumQ = currentState.getAcumQ()
				+ currentState.getQuantityPerPeriod();

	}

	private boolean checkMinCapital(double price) {

		// Applies depreciation
		nextState.capital = currentState.getCapital()
				- currentState.getDepreciationPerPeriod();

		// Meet minimum capital
		double minimalNeeds = (Double) GetParameter("minimumCapital")
				- nextState.getCapital();

		if (minimalNeeds > 0) {
			return (raiseFunds(nextState, CASH, minimalNeeds) >= minimalNeeds);
		} else {
			return true;
		}

	}

	private double raiseFunds(FirmState st, CashUsage cashUsage, double funds) {

		double fgoUsed = st.getAvailableFundsFromOperationsPerPeriod();
		double externalEquityUsed = +st.getExternalEquityAvailablePerPeriod();

		double debtUsed = st.getDebtAvailableByNewEquity(fgoUsed
				+ externalEquityUsed, cashUsage);

		// Uses pecking order
		if (fgoUsed + debtUsed + externalEquityUsed > funds) {
			fgoUsed = min(fgoUsed, funds);
			debtUsed = min(st.getDebtAvailableByNewCapital(funds, cashUsage),
					funds - fgoUsed);
			externalEquityUsed = funds - debtUsed - fgoUsed;
		}

		st.debt += debtUsed;
		st.capital += (fgoUsed + externalEquityUsed + debtUsed);
		st.setAvailableFundsFromOperationsPerPeriod(st
				.getAvailableFundsFromOperationsPerPeriod() - fgoUsed);
		st.setExternalEquityAvailablePerPeriod(st
				.getExternalEquityAvailablePerPeriod() - externalEquityUsed);

		return fgoUsed + externalEquityUsed + debtUsed;

	}

	/*
	 * Maximizes Economic Profit
	 * 
	 * includes the cost of equity invested, not cash excess
	 * 
	 * Returns the increment suggested. It is >= than zero
	 */
	private double getNetInvestment(FirmState st, double price) {
		double optCapIncr;

		optCapIncr = (Double) GetParameter("investmentParam")
				* (1 - getOptimalMarkUp() * getMarginalCost(st) / price);

		return max(0.0,
				optCapIncr * st.getCapital() + st.getDepreciationPerPeriod());

	}

	private double getMarginalCost(FirmState st) {
		int periods = (Integer) GetParameter("periods");
		double wACCxPer = st.getWACC() / periods;
		double deprecxPer = (Double) GetParameter("depreciation") / periods;

		return st.getFirstUnitCost()
				* (1.0 + st.getLRExpon())
				* pow(st.getAcumQ() + st.getQuantityPerPeriod(),
						st.getLRExpon()) + st.getMinVarCost()
				+ (wACCxPer + deprecxPer)
				/ st.getCapitalProductivityPerPeriod();

	}

	private void selectRD() {

		// Then new R&D is determined to optimize First unit cost.
		double optRD = pow(
				nextState.firstUnitCost
						/ nextState.rDEfficiency
						* (pow(nextState.acumQ + nextState.quantityPerPeriod,
								1.0 + nextState.getLRExpon()) - pow(
								nextState.acumQ, 1.0 + nextState.getLRExpon())),
				0.5) - 1.0;

		/*
		 * There is a minimum amount of RD to make FUC decrease
		 */
		nextState.rDPerPeriod = max(1.0 / nextState.rDEfficiency - 1.0, optRD);

	}

	public double getOptimalMarkUp() {
		double mktSh = getMktShare();
		double demElast = (Double) GetParameter("demandElasticity");
		double supElast = (Double) GetParameter("supplyElasticity");

		return (demElast + (1 - mktSh) * supElast)
				/ (demElast + (1 - mktSh) * supElast - mktSh);

	}

	public double getMarginalCost() {
		return getMarginalCost(currentState);
	}

	public boolean isToBeKilled() {
		return toBeKilled;
	}

	public double getWACC() {
		return currentState.getWACC();
	}

	public double getAge() {
		return (GetTickCount() - currentState.getBorn())
				/ (Integer) GetParameter("periods");
	}

	// It includes equity cost
	public double getMedCostPerPeriod() {
		return currentState.getMedCostPerPeriod();
	}

	public double getMedCostPerUnit() {
		return getMedCostPerPeriod() / getQuantityPerPeriod();
	}

	public double getEBITPerPeriod() {
		return currentState.getEBITPerPeriod();
	}

	public double getEBITDAPerPeriod() {
		return currentState.getEBITDAPerPeriod();
	}

	public double getInterestPerPeriod() {
		return currentState.getInterestPerPeriod();
	}

	public double getExpectedEquityRetributionPerPeriod() {
		return currentState.getExpectedEquityRetributionPerPeriod();
	}

	public double getExpectedCapitalRetributionPerPeriod() {
		return currentState.getExpectedCapitalRetributionPerPeriod();
	}

	public double getTotFixedCostPerPeriod() {
		return currentState.getTotFixedCostPerPeriod();
	}

	public double getTotVarCostPerPeriod() {
		return currentState.getTotVarCostPerPeriod();
	}

	public double getROE() {
		return currentState.getROE();
	}

	public double getROI() {
		return currentState.getROI();
	}

	public double getProfitPerPeriod() {
		return currentState.getProfitPerPeriod();
	}

	public double getPrice() {
		return supplyManager.price;
	}

	public double getCapital() {
		return currentState.getCapital();
	}

	public double getCash() {
		return currentState.getCash();
	}

	public double getAssets() {
		return currentState.getAssets();
	}

	public double getDebt() {
		return currentState.getDebt();
	}

	public double getNetDebt() {
		return currentState.getNetDebt();
	}

	public double getLeverage() {
		return currentState.getLeverage();
	}

	public double getNetLeverage() {
		return currentState.getNetLeverage();
	}

	public double getEquity() {
		return currentState.getEquity();
	}

	public double getPerformance() {
		return currentState.getPerformance();
	}

	public double getDepreciationPerPeriod() {
		return currentState.getDepreciationPerPeriod();
	}

	public double getFirstUnitCost() {
		return currentState.getFirstUnitCost();
	}

	public double getQuantityPerPeriod() {
		return currentState.getQuantityPerPeriod();
	}

	public double getSalesPerPeriod() {
		return currentState.getQuantityPerPeriod() * supplyManager.price;
	}

	public double getMktShare() {
		return currentState.getQuantityPerPeriod()
				/ supplyManager.totalQuantityPerPeriod;
	}

	public double getAcumQ() {
		return currentState.getAcumQ();
	}

	public double getRDPerPeriod() {
		return currentState.getRDPerPeriod();
	}

	public double getInitialFUC() {
		return currentState.getInitialFUC();
	}

	public double getRDEfficiency() {
		return currentState.getRDEfficiency();
	}

	public double getTargetLeverage() {
		return currentState.getTargetLeverage();
	}

	public double getMaxExternalEquity() {
		return currentState.getMaxExternalEquity();
	}

	public double getLearningRate() {
		return currentState.getLearningRate();
	}

	public double getMinVarCost() {
		return currentState.getMinVarCost();
	}

	public double getFixedCostPerPeriod() {
		return currentState.getFixedCostPerPeriod();
	}

	public double getBorn() {
		return currentState.getBorn();
	}

	public double getBornInYears() {
		return currentState.getBornInYears();
	}

	public String toString() {
		return this.agentID;
	}

	public double getIndepVarValue(IndepVarsNames key) {
		return currentState.getIndepVarValue(key);
	}

	public int getCohort(IndepVarsNames key) {
		return getCohort(getIndepVarValue(key),
				independentVarsManager.getCohortLimit(key));
	}

	private int getCohort(double fVal, double[] lim) {

		for (int i = 0; i < independentVarsManager.cohorts - 1; i++) {
			if (fVal < lim[i])
				return i + 1;
		}

		return independentVarsManager.cohorts;

	}

	public double getField(String var) {
		Field f = null;
		try {
			f = Class.forName("suddenStop.FirmState").getDeclaredField(var);
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(-1);
		}

		try {
			return f.getDouble(this.currentState);
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return 0;

	}

	public double get(String var) {
		Method m = null;
		try {
			m = Class.forName("suddenStop.FirmState").getDeclaredMethod(var);
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(-1);
		}

		try {
			return (Double) m.invoke(this);
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return 0;

	}
}
