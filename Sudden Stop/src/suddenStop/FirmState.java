package suddenStop;

import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.pow;
import static repast.simphony.essentials.RepastEssentials.GetParameter;
import static repast.simphony.essentials.RepastEssentials.GetTickCount;
import static suddenStop.CashUsage.ONLY_CASH;

public class FirmState implements Cloneable {

	double initialFUC;
	double rDEfficiency;
	double targetLeverage;
	double maxExternalEquity;
	double learningRate;
	/*
	 * capital = debt + equity debt could be <0 meaning cash excess
	 */
	double capital;
	double debt;
	double firstUnitCost;
	double acumQ;
	double acumProfit;
	double profitPerPeriod;
	double quantityPerPeriod;

	private double born;

	private double rDPerPeriod;
	private double availableFundsFromOperationsPerPeriod;
	private double externalEquityAvailablePerPeriod;
	private double capitalProductivity;
	private double minVarCost;
	private double performance;

	public FirmState() {

		/*
		 * Obtain random variables
		 */
		// A minimum FUC is set to 10% of mean
		firstUnitCost = max(0.1 * (Double) GetParameter("firstUnitCostMean"),
				Firm.supplyManager.fUCDistrib.nextDouble());
		initialFUC = firstUnitCost;

		rDEfficiency = max(0.0,
				Firm.supplyManager.rDEfficiencyDistrib.nextDouble());
		targetLeverage = Firm.supplyManager.targetLeverageDistrib.nextDouble();
		learningRate = Firm.supplyManager.learningRateDistrib.nextDouble();

		/*
		 * Equity and Capital.
		 * 
		 * Equity is chosen according to distribution, truncated so capital
		 * won't be lower than minimum
		 */
		double minEquity = (Double) GetParameter("minimumCapital")
				* (1 - targetLeverage);
		double equity = max(minEquity,
				Firm.supplyManager.iniEquityDistrib.nextDouble());
		capital = equity / (1 - targetLeverage);

		/*
		 * Read constant state variables
		 */
		capitalProductivity = (Double) GetParameter("capitalProductivity");
		minVarCost = (Double) GetParameter("minVarCost");

		/*
		 * Initialize the remaining variables
		 */
		born = GetTickCount();
		debt = capital * targetLeverage;
		rDPerPeriod = 0.0; // There is no R&D until it is planned
		acumQ = 0.0;
		quantityPerPeriod = 0.0;
		profitPerPeriod = 0.0;
		availableFundsFromOperationsPerPeriod = 0.0;
		externalEquityAvailablePerPeriod = 0.0;
		/*
		 * Initial performance is set to minimum performance. So, when
		 * performance is checked, it will be higher than minimum if and only if
		 * profit is higher than minimum performance
		 */
		performance = getMinimumPerformance();

	}

	@Override
	public FirmState clone() {
		try {
			return (FirmState) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}

	public double getLRExpon() {
		return log(learningRate) / log(2.0);
	}

	public void resetExternalEquityAvailable() {
		int periods = (Integer) GetParameter("periods");
		
		if (Demand.isSS()) {
			externalEquityAvailablePerPeriod = 0.0;
		} else {
			externalEquityAvailablePerPeriod = getMaxExternalEquity()
			* getCapital() / periods;
		}

	}

	private double getMaxExternalEquity() {
		return (Double) GetParameter("maxExternalEquity");		
	}

	public double getDebtAvailableByNewEquity(double newEquity,
			CashUsage cashUsage) {
		return getDebtAvailable(newEquity, true, cashUsage);
	}

	public double getDebtAvailableByNewCapital(double newEquity,
			CashUsage cashUsage) {
		return getDebtAvailable(newEquity, false, cashUsage);
	}

	/*
	 * Returns the new debt available assuming newEquity is raised
	 * 
	 * The value returned could be negative meaning that more equity than
	 * newEquity needs to be raised in order to fulfill leverage constrain
	 */
	private double getDebtAvailable(double newFunds, boolean isNewEquity,
			CashUsage cashUsage) {
		double newDebt = 0;
		double totalLeverage;

		/*
		 * available leverage is the debt capacity not used plus the extra cash
		 * over the cash needed according to policy
		 * 
		 * availLeverage could be < 0 meaning the leverage constrain is not
		 * currently fulfilled
		 */
		double availLeverage = getTargetLeverage() * getCapital() - getDebt()
				+ getCash();

		if (isNewEquity) {
			// newFunds is equity increase
			totalLeverage = (availLeverage + newFunds * getTargetLeverage())
					/ (1 - getTargetLeverage());
		} else {
			// newFunds is capital increase
			totalLeverage = availLeverage + getTargetLeverage() * newFunds;
		}

		if (Demand.isSS())
			cashUsage = ONLY_CASH;

		switch (cashUsage) {
		case LEVERAGE:
			newDebt = totalLeverage;
			break;

		case ONLY_CASH:
			newDebt = getCash();
			break;

		case CASH:
			newDebt = max(getCash(), totalLeverage);

		}

		return newDebt;

	}

	/*
	 * Expected Capital retribution includes interest and invested equity
	 * retribution It does not include cost of cash excess.
	 * 
	 * By assuming M&M, it is assumed that cash excess receives a proper return
	 * according to risk
	 */
	public double getMedCost() {
		return (getTotVarCostPerPeriod() + getTotFixedCostPerPeriod() + getExpectedCapitalRetributionPerPeriod())
				/ getQuantityPerPeriod();
	}

	// Calculates cost using learning curve: cost of new acummulated Q minus
	// old acummulated Q. See http://maaw.info/LearningCurveSummary.htm
	// (Wright model)
	public double getTotVarCostPerPeriod() {

		return firstUnitCost
				* (pow(getAcumQ() + getQuantityPerPeriod(), 1.0 + getLRExpon()) - pow(
						getAcumQ(), 1.0 + getLRExpon())) + getMinVarCost()
				* getQuantityPerPeriod();

	}

	public double getTotFixedCostPerPeriod() {

		return getDepreciationPerPeriod() + getRDPerPeriod();

	}

	public double getMinimumPerformance() {
		return getWACC();
	}

	public double getCashFixedCostsPerPeriod() {
		return getRDPerPeriod();
	}

	public double getProfitPerPeriod() {
		return profitPerPeriod;
	}

	public double getCapital() {
		return capital;
	}

	public double getDebt() {
		return max(0.0, debt);
	}

	public double getCash() {
		return max(0.0, -debt);
	}

	public double getEquity() {
		return capital - debt;
	}

	public double getLeverage() {
		return getDebt() / getAssets();
	}

	public double getNetLeverage() {
		return debt / capital;
	}

	public double getAssets() {
		return getCapital() + getCash();
	}

	public double getWACC() {
		return (Double) GetParameter("wACC");
	}

	public double getAcumQ() {
		return acumQ;
	}

	public double getAcumProfit() {
		return acumProfit;
	}

	public double getFirstUnitCost() {
		return firstUnitCost;
	}

	public double getInitialFUC() {
		return initialFUC;
	}

	public double getTargetLeverage() {
		return targetLeverage;
	}

	public double getCapitalProductivityPerPeriod() {
		return getCapitalProductivity() / (Integer) GetParameter("periods");
	}

	public double getCostOfDebt() {
		return (Double) GetParameter("costOfDebt");
	}

	// It is assumed M&M, ie. WACC is constant
	public double getCostOfEquity() {
		return getWACC() + (getWACC() - getCostOfDebt())
				* (1 + getDebt() / getEquity());
	}

	public double getMinVarCost() {
		return minVarCost;
	}

	public double getEBITDAPerPeriod() {
		return getProfitPerPeriod() + getInterestPerPeriod()
				+ getDepreciationPerPeriod();
	}

	public double getEBITPerPeriod() {
		return getProfitPerPeriod() + getInterestPerPeriod();
	}

	public double getInterestPerPeriod() {
		return getCostOfDebt() / (Integer) GetParameter("periods") * getDebt();
	}

	public double getExpectedEquityRetributionPerPeriod() {
		return getCostOfEquity() * getEquity()
				/ (Integer) GetParameter("periods");
	}

	public double getExpectedCapitalRetributionPerPeriod() {
		return getWACC() * getCapital() / (Integer) GetParameter("periods");
	}

	public double getDepreciationPerPeriod() {
		return (Double) GetParameter("depreciation") * getCapital()
				/ (Integer) GetParameter("periods");
	}

	public double getROE() {
		return getProfitPerPeriod() * (Integer) GetParameter("periods")
				/ getEquity();
	}

	public double getROI() {
		return getEBITPerPeriod() * (Integer) GetParameter("periods")
				/ getCapital();
	}

	public double getRONA() {
		return getEBITPerPeriod() * (Integer) GetParameter("periods")
				/ getAssets();
	}

	public double getPerformance() {
		return performance;
	}

	public double getBorn() {
		return born;
	}

	public double getBornInYears() {
		return born / (Integer) GetParameter("periods");
	}

	public void setBorn(double born) {
		this.born = born;
	}

	public void setAcumQ(double acumQ) {
		this.acumQ = acumQ;
	}

	public void setAcumProfit(double acumProfit) {
		this.acumProfit = acumProfit;
	}

	public void setFirstUnitCost(double firstUnitCost) {
		this.firstUnitCost = firstUnitCost;
	}

	public void setCapital(double capital) {
		this.capital = capital;
	}

	public void setDebt(double debt) {
		this.debt = debt;
	}

	public void setTargetLeverage(double targetLeverage) {
		this.targetLeverage = targetLeverage;
	}

	public void setrDEfficiency(double rDEfficiency) {
		this.rDEfficiency = rDEfficiency;
	}

	public void setProfit(double profit) {
		this.profitPerPeriod = profit;
	}

	public void setPerformance(double performance) {
		this.performance = performance;
	}

	public double getQuantityPerPeriod() {
		return quantityPerPeriod;
	}

	public double getRDPerPeriod() {
		return rDPerPeriod;
	}

	public double setRDPerPeriod(double rDPerPeriod) {
		return this.rDPerPeriod = rDPerPeriod;
	}

	public double getExternalEquityAvailablePerPeriod() {
		return externalEquityAvailablePerPeriod;
	}

	public double getAvailableFundsFromOperationsPerPeriod() {
		return availableFundsFromOperationsPerPeriod;
	}

	public double getNetDebt() {
		return debt;
	}

	public double getRDEfficiency() {
		return rDEfficiency;
	}

	public double getLearningRate() {
		return learningRate;
	}

	public double getCapitalProductivity() {
		return capitalProductivity;
	}

	public void setAvailableFundsFromOperationsPerPeriod(
			double availableFundsFromOperations) {
		this.availableFundsFromOperationsPerPeriod = availableFundsFromOperations;
	}

	public void setExternalEquityAvailablePerPeriod(
			double externalEquityAvailablePerPeriod) {
		this.externalEquityAvailablePerPeriod = externalEquityAvailablePerPeriod;
	}

}