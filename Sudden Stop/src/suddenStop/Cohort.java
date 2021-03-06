package suddenStop;

public class Cohort {
	Firm f;

	public Cohort(Firm f) {
		this.f = f;
	}

	public double getOptimalMarkUp() {
		return f.getOptimalMarkUp();
	}

	public double getWACC() {
		return f.getWACC();
	}

	public double getAge() {
		return f.getAge();
	}

	public double getEBITPerPeriod() {
		return f.getEBITPerPeriod();
	}

	public double getEBITDAPerPeriod() {
		return f.getEBITDAPerPeriod();
	}

	public double getInterestPerPeriod() {
		return f.getInterestPerPeriod();
	}

	public double getExpectedEquityRetributionPerPeriod() {
		return f.getExpectedEquityRetributionPerPeriod();
	}

	public double getExpectedCapitalRetributionPerPeriod() {
		return f.getExpectedCapitalRetributionPerPeriod();
	}

	public double getTotFixedCostPerPeriod() {
		return f.getTotFixedCostPerPeriod();
	}
	
	public double getCashFixedCostsPerPeriod(){
		return f.getCashFixedCostsPerPeriod();
	}

	public double getTotVarCostPerPeriod() {
		return f.getTotVarCostPerPeriod();
	}

	public double getROE() {
		return f.getROE();
	}

	public double getROI() {
		return f.getROI();
	}

	public double getRONA() {
		return f.getRONA();
	}

	public double getProfitPerPeriod() {
		return f.getProfitPerPeriod();
	}

	public double getCapital() {
		return f.getCapital();
	}

	public double getCash() {
		return f.getCash();
	}

	public double getAssets() {
		return f.getAssets();
	}

	public double getDebt() {
		return f.getDebt();
	}

	public double getNetDebt() {
		return f.getNetDebt();
	}

	public double getLeverage() {
		return f.getLeverage();
	}

	public double getNetLeverage() {
		return f.getNetLeverage();
	}

	public double getEquity() {
		return f.getEquity();
	}

	public double getPerformance() {
		return f.getPerformance();
	}

	public double getDepreciationPerPeriod() {
		return f.getDepreciationPerPeriod();
	}

	public double getDepreciationPerUnit() {
		return f.getDepreciationPerPeriod() / f.getQuantityPerPeriod();
	}

	public double getFirstUnitCost() {
		return f.getFirstUnitCost();
	}

	public double getQuantityPerPeriod() {
		return f.getQuantityPerPeriod();
	}

	public double getSalesPerPeriod() {
		return f.getSalesPerPeriod();
	}

	public double getMktShare() {
		return f.getMktShare();
	}

	public double getAcumQ() {
		return f.getAcumQ();
	}

	public double getRDPerPeriod() {
		return f.getRDPerPeriod();
	}

	public double getInitialFUC() {
		return f.getInitialFUC();
	}

	public double getRDEfficiency() {
		return f.getRDEfficiency();
	}

	public double getTargetLeverage() {
		return f.getTargetLeverage();
	}

	public double getLearningRate() {
		return f.getLearningRate();
	}

	public double getMinVarCost() {
		return f.getMinVarCost();
	}

	public double getBorn() {
		return f.getBorn();
	}

	public double getBornInYears() {
		return f.getBornInYears();
	}

	public double getMedCost() {
		return f.getMedCost();
	}

	public double getMarginalCost() {
		return f.getMarginalCost();
	}

	public double getInterestPerUnit() {
		return getInterestPerPeriod() / getQuantityPerPeriod();
	}

	public double getMedCostCash() {

		return getMedCost() - getDepreciationPerUnit()
				- getExpectedCapitalRetributionPerUnit() + getInterestPerUnit();

	}

	public double getVarCostPerUnit() {
		return getTotVarCostPerPeriod() / getQuantityPerPeriod();
	}

	public double getCashFixedCostsPerUnit() {
		return getCashFixedCostsPerPeriod() / getQuantityPerPeriod();
	}

	public double getNoCashFixedCostsPerUnit() {
		return getDepreciationPerPeriod() / getQuantityPerPeriod();
	}

	public double getExpectedEquityRetributionPerUnit() {
		return getExpectedEquityRetributionPerPeriod() / getQuantityPerPeriod();
	}

	public double getExpectedCapitalRetributionPerUnit() {
		return getExpectedCapitalRetributionPerPeriod()
				/ getQuantityPerPeriod();
	}

	public double getUpToVarCostPerUnitCash() {
		return getCashFixedCostsPerUnit() + getVarCostPerUnit();
	}

	public double getCashPerUnit() {
		return getCash() / getQuantityPerPeriod();
	}

	public double getCashNeedsPerUnit() {
		return getMedCostCash() - getCashPerUnit();
	}

	public double getRDPerUnit(){
		return getRDPerPeriod()/ getQuantityPerPeriod();
	}
	
	public String toString() {
		return f.agentID;
	}

}
