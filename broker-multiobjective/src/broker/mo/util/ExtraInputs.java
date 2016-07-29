package broker.mo.util;

import java.math.BigDecimal;

/**
 * 
 * @author Lino Chamorro
 * 
 */
public class ExtraInputs {

	// restrictions
	public BigDecimal vmToDeploy;
	public BigDecimal cpuMin;
	public BigDecimal cpuMax;
	public BigDecimal memMin;
	public BigDecimal memMax;
	public BigDecimal budgetMin;
	public BigDecimal budgetMax;
	public BigDecimal cpuExpected;
	public BigDecimal memExpected;
	public BigDecimal budgetExpected;
	public BigDecimal loadBalanceMin;
	public BigDecimal loadBalanceMax;
	public BigDecimal percent;
	// population
	public BigDecimal population;
	// generations
	public BigDecimal generations;
	// for tournament
	public BigDecimal objectivesFunctions;
	public boolean softConstraints;
	public String softConstraintsType;
	public boolean printOverhead;
	public String selectionMode;

}
