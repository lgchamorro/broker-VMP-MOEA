package broker.mo.util;

import java.math.BigDecimal;

/**
 * 
 * @author Lino Chamorro
 * 
 */
public class DCSConstants {

	public static final String PROVIDERS_BEGIN = "PROVIDERS-BEGIN";
	public static final String PROVIDERS_END = "PROVIDERS-END";
	public static final String INSTANCE_TYPES_BEGIN = "INSTANCE-TYPES-BEGIN";
	public static final String INSTANCE_TYPES_END = "INSTANCE-TYPES-END";
	public static final String LABEL = "LABEL";
	public static final String CORES = "CORES";
	public static final String MEMORY = "MEMORY";
	public static final String PRICES = "PRICES";
	public static final String OVERHEAD_BEGIN = "OVERHEAD-BEGIN";
	public static final String OVERHEAD_END = "OVERHEAD-END";
	public static final String ALLOCATION = "ALLOC";
	public static final String DEALLOCATION = "DEALLOC";
	public static final String RESTRICTION_BEGIN = "RESTRICTIONS-BEGIN";
	public static final String RESTRICTION_END = "RESTRICTIONS-END";
	public static final String VM_TO_DEPLOY = "VIRTUAL-MACHINES-TO-DEPLOY";
	public static final String CPU_MIN = "CPU_MIN";
	public static final String CPU_MAX = "CPU_MAX";
	public static final String CPU_EXPECTED = "CPU_EXPECTED";
	public static final String MEM_MIN = "MEM_MIN";
	public static final String MEM_MAX = "MEM_MAX";
	public static final String MEM_EXPECTED = "MEM_EXPECTED";
	public static final String BUDGET_MIN = "BUDGET_MIN";
	public static final String BUDGET_MAX = "BUDGET_MAX";
	public static final String BUDGET_EXPECTED = "BUDGET_EXPECTED";
	public static final String LOAD_BALANCE_MIN = "LOAD-BALANCE-MIN";
	public static final String LOAD_BALANCE_MAX = "LOAD-BALANCE-MAX";
	public static final String NOT_APPLICABLE = "N/A";
	public static final String FUNCTION_BEGIN = "FUNCTION-BEGIN";
	public static final String POPULATION = "POPULATION";
	public static final String GENERATIONS = "GENERATIONS";
	public static final String SOFT_CONSTRAINTS = "SOFT-CONSTRAINTS";
	public static final String SOFT_CONSTRAINTS_TYPE = "SOFT_CONSTRAINTS_TYPE";
	public static final String SOFT_RANGE = "SOFT_RANGE";
	public static final String SOFT_PERCENT = "SOFT_PERCENT";
	public static final String PERCENT = "PERCENT";
	public static final String PRINT_OVERHEAD = "PRINT-OVERHEAD";
	public static final String OBJECTIVES_FUNCTIONS = "OBJECTIVES_FUNCTIONS";
	public static final String FUNCTION_END = "FUNCTION-END";
	public static final String TRUE = "true";
	public static final String FALSE = "false";
	public static final String SELECTION_MODE = "SELECTION-MODE";
	public static final String ALEATORY = "ALEATORY";
	public static final String DISTANCE = "DISTANCE";
	public static final String PREFERRED_CPU = "PREFERRED_CPU";
	public static final String PREFERRED_MEM = "PREFERRED_MEM";
	public static final String PREFERRED_PRICE = "PREFERRED_PRICE";
	public static final String PREFERRED_LOC = "PREFERRED_LOC";
	public static final String PREFERRED_OBJECTIVES = "PREFERED_OBJECTIVES";
	//
	public static final int MAX_RANDOM = 1000;
	public static final int INFINITY = 999999999;
	public static final int ENE_TOURNAMENT = 2;
	//
	public static final String SEPARATOR = ";";
	//
	public static final int VM_TO_DEPLOY_INDEX = 0;
	public static final int CPU_MIN_INDEX = 1;
	public static final int CPU_MAX_INDEX = 2;
	public static final int MEM_MIN_INDEX = 3;
	public static final int MEM_MAX_INDEX = 4;
	public static final int BUDGET_MIN_INDEX = 5;
	public static final int BUDGET_MAX_INDEX = 6;
	public static final int LOAD_BALANCE_MIN_INDEX = 7;
	public static final int LOAD_BALANCE_MAX_INDEX = 8;
	//
	public static final BigDecimal MAX_PRICE = new BigDecimal(999999);

}
