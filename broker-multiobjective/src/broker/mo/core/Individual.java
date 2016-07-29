package broker.mo.core;

import java.math.BigDecimal;
import java.util.List;

import org.apache.log4j.Logger;

import broker.mo.util.DCSConstants;
import broker.mo.util.ExtraInputs;

/**
 * 
 * @author Lino Chamorro
 * 
 */
public class Individual implements Comparable<Individual> {

	private Logger log = Logger.getLogger(getClass());

	int n;
	String[] instanceName;
	BigDecimal[] instanceCores;
	BigDecimal[] instanceMemory;
	BigDecimal[][] prices;
	String[] providers;
	Individual prevSolution;
	ExtraInputs extraInputs;
	/**
	 * current solution. j = VMi. output[0][j]: InstanceType. output[1][j]:
	 * cloudprovider
	 */
	BigDecimal[][] output;
	BigDecimal[] migrationOverheadCore;
	BigDecimal[] migrationOverheadMemory;
	BigDecimal[] migrationTime;
	BigDecimal[][] downtimeStatistics;

	/**
	 * Total Infrastructure Capacity
	 */
	BigDecimal tic;
	/**
	 * Total Infrastructure Memory
	 */
	BigDecimal tim;
	/**
	 * Total Infrastructure Core less overhead
	 */
	BigDecimal ticWithOverhead;
	/**
	 * Total Infrastructure Memory less overhead
	 */
	BigDecimal timWithOverhead;

	/**
	 * Total Infrastructure Price
	 */
	BigDecimal tip;
	BigDecimal mcCore;
	BigDecimal mcMemory;
	/**
	 * aux output structures
	 */
	int[] instanceSummary;
	BigDecimal[][][] instancePerProviderSummary;

	/**
	 * Load Balance
	 */
	BigDecimal[] loc;

	/**
	 * NSGA Parameter
	 */
	int rank;
	BigDecimal crowdedDistance;
	List<Individual> sP;
	int nP;

	public Individual(BigDecimal[][] output) {
		super();
		this.output = output;
		ticWithOverhead = new BigDecimal(0);
		mcCore = new BigDecimal(0);
		timWithOverhead = new BigDecimal(0);
		mcMemory = new BigDecimal(0);
	}

	@Override
	public String toString() {
		BigDecimal c = crowdedDistance == null ? new BigDecimal(0)
				: crowdedDistance;
		StringBuilder sb = new StringBuilder();
		sb.append("Individual [tic=" + tic + ", tim=" + tim + ", mcCore="
				+ mcCore + ", mcMemory=" + mcMemory + ", ticWithOverhead="
				+ ticWithOverhead + ", timWithOverhead=" + timWithOverhead
				+ ", tip=" + tip + ", crowdedDistance=" + c + ", LOCmin: "
				+ this.getMinLoadIndex() + ", LOC[");
		for (int i = 0; i < loc.length; i++) {
			sb.append(loc[i]);
			if (i < loc.length - 1) {
				sb.append(", ");
			}
		}
		sb.append("]]");
		return sb.toString();
	}

	public int getMinLoadIndex() {
		int minLoadIndex = 99999;
		for (int i = 0; i < loc.length; i++) {
			if (minLoadIndex >= loc[i].intValue()) {
				minLoadIndex = loc[i].intValue();
			}
		}
		return minLoadIndex;
	}

	@Override
	public int compareTo(Individual individual) {
		log.trace("Comparing. \n\t" + this + "\n\t" + individual);
		/*****************************************
		 * DOMINATES
		 *****************************************/
		if (individual == null || this.dominates(individual)) {
			return 1;
		}

		/*****************************************
		 * NO DOMINATE
		 *****************************************/
		if (this.nonDominates(individual)) {
			return 0;
		}

		/*****************************************
		 * DOMINATED BY
		 *****************************************/
		if (this.dominatedBy(individual)) {
			return -1;
		}
		log.error("Not comparable. \n\t" + this + "\n\t" + individual);
		return -2;
	}

	private boolean dominates(Individual individual) {
		/*
		 * best in all objectives (1)
		 */
		// best, best, best, best
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) > 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) > 0
				&& this.tip.compareTo(individual.tip) < 0
				&& this.getMinLoadIndex() > individual.getMinLoadIndex()) {
			return true;
		}

		/*
		 * best in at least a objective and equals to others ones (4)
		 */
		// best, equal, equal, equal
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) > 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) == 0
				&& this.tip.compareTo(individual.tip) == 0
				&& this.getMinLoadIndex() == individual.getMinLoadIndex()) {
			return true;
		}
		// equal, best, equal, equal
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) > 0
				&& this.tip.compareTo(individual.tip) == 0
				&& this.getMinLoadIndex() == individual.getMinLoadIndex()) {
			return true;
		}
		// equal, equal, best, equal
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) == 0
				&& this.tip.compareTo(individual.tip) < 0
				&& this.getMinLoadIndex() == individual.getMinLoadIndex()) {
			return true;
		}
		// equal, equal, equal, best
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) == 0
				&& this.tip.compareTo(individual.tip) == 0
				&& this.getMinLoadIndex() > individual.getMinLoadIndex()) {
			return true;
		}

		/*
		 * best in two objective and equals to others (6)
		 */
		// best, best, equal, equal
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) > 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) > 0
				&& this.tip.compareTo(individual.tip) == 0
				&& this.getMinLoadIndex() == individual.getMinLoadIndex()) {
			return true;
		}
		// best, equal, best, equal
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) > 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) == 0
				&& this.tip.compareTo(individual.tip) < 0
				&& this.getMinLoadIndex() == individual.getMinLoadIndex()) {
			return true;
		}
		// best, equal, equal, best
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) > 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) == 0
				&& this.tip.compareTo(individual.tip) == 0
				&& this.getMinLoadIndex() > individual.getMinLoadIndex()) {
			return true;
		}
		// equal, best, best, equal
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) > 0
				&& this.tip.compareTo(individual.tip) < 0
				&& this.getMinLoadIndex() == individual.getMinLoadIndex()) {
			return true;
		}
		// equal, best, equal, best
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) > 0
				&& this.tip.compareTo(individual.tip) == 0
				&& this.getMinLoadIndex() > individual.getMinLoadIndex()) {
			return true;
		}
		// equal, equal, best, best
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) == 0
				&& this.tip.compareTo(individual.tip) < 0
				&& this.getMinLoadIndex() > individual.getMinLoadIndex()) {
			return true;
		}

		/*
		 * best in three objective and equals to one (4)
		 */
		// best, best, best, equal
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) > 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) > 0
				&& this.tip.compareTo(individual.tip) < 0
				&& this.getMinLoadIndex() == individual.getMinLoadIndex()) {
			return true;
		}
		// best, best, equal, best
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) > 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) > 0
				&& this.tip.compareTo(individual.tip) == 0
				&& this.getMinLoadIndex() > individual.getMinLoadIndex()) {
			return true;
		}
		// best, equal, best, best
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) > 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) == 0
				&& this.tip.compareTo(individual.tip) < 0
				&& this.getMinLoadIndex() > individual.getMinLoadIndex()) {
			return true;
		}
		// equal, best, best, best
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) > 0
				&& this.tip.compareTo(individual.tip) < 0
				&& this.getMinLoadIndex() > individual.getMinLoadIndex()) {
			return true;
		}

		/*
		 * *** equal (1) ***
		 */
		// equal, equal, equal, equal
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) == 0
				&& this.tip.compareTo(individual.tip) == 0
				&& this.getMinLoadIndex() == individual.getMinLoadIndex()) {
			return true;
		}
		return false;
	}

	private boolean nonDominates(Individual individual) {
		/*
		 * best in at least a objective and worst to others ones (4)
		 */
		// best, worst, worst, worst
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) > 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) < 0
				&& this.tip.compareTo(individual.tip) > 0
				&& this.getMinLoadIndex() < individual.getMinLoadIndex()) {
			return true;
		}
		// worst, best, worst, worst
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) < 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) > 0
				&& this.tip.compareTo(individual.tip) > 0
				&& this.getMinLoadIndex() < individual.getMinLoadIndex()) {
			return true;
		}
		// worst, worst, best, worst
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) < 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) < 0
				&& this.tip.compareTo(individual.tip) < 0
				&& this.getMinLoadIndex() < individual.getMinLoadIndex()) {
			return true;
		}
		// worst, worst, worst, best
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) < 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) < 0
				&& this.tip.compareTo(individual.tip) > 0
				&& this.getMinLoadIndex() > individual.getMinLoadIndex()) {
			return true;
		}

		/*
		 * best in two objective and worst to others ones (6)
		 */
		// best, best, worst, worst
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) > 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) > 0
				&& this.tip.compareTo(individual.tip) > 0
				&& this.getMinLoadIndex() < individual.getMinLoadIndex()) {
			return true;
		}
		// best, worst, best, worst
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) > 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) < 0
				&& this.tip.compareTo(individual.tip) < 0
				&& this.getMinLoadIndex() < individual.getMinLoadIndex()) {
			return true;
		}
		// best, worst, worst, best
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) > 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) < 0
				&& this.tip.compareTo(individual.tip) > 0
				&& this.getMinLoadIndex() > individual.getMinLoadIndex()) {
			return true;
		}
		// worst, best, best, worst
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) < 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) > 0
				&& this.tip.compareTo(individual.tip) < 0
				&& this.getMinLoadIndex() < individual.getMinLoadIndex()) {
			return true;
		}
		// worst, best, worst, best
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) < 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) > 0
				&& this.tip.compareTo(individual.tip) > 0
				&& this.getMinLoadIndex() > individual.getMinLoadIndex()) {
			return true;
		}
		// worst, worst, best, best
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) < 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) < 0
				&& this.tip.compareTo(individual.tip) < 0
				&& this.getMinLoadIndex() > individual.getMinLoadIndex()) {
			return true;
		}

		/*
		 * best in three objective and worst to remain one (4)
		 */
		// best, best, best, worst
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) > 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) > 0
				&& this.tip.compareTo(individual.tip) < 0
				&& this.getMinLoadIndex() < individual.getMinLoadIndex()) {
			return true;
		}
		// best, best, worst, best
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) > 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) > 0
				&& this.tip.compareTo(individual.tip) > 0
				&& this.getMinLoadIndex() > individual.getMinLoadIndex()) {
			return true;
		}
		// best, worst, best, best
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) > 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) < 0
				&& this.tip.compareTo(individual.tip) < 0
				&& this.getMinLoadIndex() > individual.getMinLoadIndex()) {
			return true;
		}
		// worst, best, best, best
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) < 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) > 0
				&& this.tip.compareTo(individual.tip) < 0
				&& this.getMinLoadIndex() > individual.getMinLoadIndex()) {
			return true;
		}

		/*
		 * equal in a objective, best in two and worst in one (12)
		 */
		// first objective equal
		// equal, best, best, worst
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) > 0
				&& this.tip.compareTo(individual.tip) < 0
				&& this.getMinLoadIndex() < individual.getMinLoadIndex()) {
			return true;
		}
		// equal, best, worst, best
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) > 0
				&& this.tip.compareTo(individual.tip) > 0
				&& this.getMinLoadIndex() > individual.getMinLoadIndex()) {
			return true;
		}
		// equal, worst, best, best
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) < 0
				&& this.tip.compareTo(individual.tip) < 0
				&& this.getMinLoadIndex() > individual.getMinLoadIndex()) {
			return true;
		}

		// second objective equal
		// best, equal, best, worst
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) > 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) == 0
				&& this.tip.compareTo(individual.tip) < 0
				&& this.getMinLoadIndex() < individual.getMinLoadIndex()) {
			return true;
		}
		// best, equal, worst, best
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) > 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) == 0
				&& this.tip.compareTo(individual.tip) > 0
				&& this.getMinLoadIndex() > individual.getMinLoadIndex()) {
			return true;
		}
		// worst, equal, best, best
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) < 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) == 0
				&& this.tip.compareTo(individual.tip) < 0
				&& this.getMinLoadIndex() > individual.getMinLoadIndex()) {
			return true;
		}

		// third objective equal
		// best, best, equal, worst
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) > 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) > 0
				&& this.tip.compareTo(individual.tip) == 0
				&& this.getMinLoadIndex() < individual.getMinLoadIndex()) {
			return true;
		}
		// best, worst, equal, best
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) > 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) < 0
				&& this.tip.compareTo(individual.tip) == 0
				&& this.getMinLoadIndex() > individual.getMinLoadIndex()) {
			return true;
		}
		// worst, best, equal, best
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) < 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) > 0
				&& this.tip.compareTo(individual.tip) == 0
				&& this.getMinLoadIndex() > individual.getMinLoadIndex()) {
			return true;
		}

		// fourth objective equal
		// best, best, worst, equal
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) > 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) > 0
				&& this.tip.compareTo(individual.tip) > 0
				&& this.getMinLoadIndex() == individual.getMinLoadIndex()) {
			return true;
		}
		// best, worst, best, equal
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) > 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) < 0
				&& this.tip.compareTo(individual.tip) < 0
				&& this.getMinLoadIndex() == individual.getMinLoadIndex()) {
			return true;
		}
		// worst, best, best, equal
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) < 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) > 0
				&& this.tip.compareTo(individual.tip) < 0
				&& this.getMinLoadIndex() == individual.getMinLoadIndex()) {
			return true;
		}

		/*
		 * equal in a objective, best in one and worst in two (12)
		 */
		// first objective equal
		// equal, worst, worst, best
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) < 0
				&& this.tip.compareTo(individual.tip) > 0
				&& this.getMinLoadIndex() > individual.getMinLoadIndex()) {
			return true;
		}
		// equal, worst, best, worst
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) < 0
				&& this.tip.compareTo(individual.tip) < 0
				&& this.getMinLoadIndex() < individual.getMinLoadIndex()) {
			return true;
		}
		// equal, best, worst, worst
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) > 0
				&& this.tip.compareTo(individual.tip) > 0
				&& this.getMinLoadIndex() < individual.getMinLoadIndex()) {
			return true;
		}

		// second objective equal
		// worst, equal, worst, best
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) < 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) == 0
				&& this.tip.compareTo(individual.tip) > 0
				&& this.getMinLoadIndex() > individual.getMinLoadIndex()) {
			return true;
		}
		// worst, equal, best, worst
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) < 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) == 0
				&& this.tip.compareTo(individual.tip) < 0
				&& this.getMinLoadIndex() < individual.getMinLoadIndex()) {
			return true;
		}
		// best, equal, worst, worst
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) > 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) == 0
				&& this.tip.compareTo(individual.tip) > 0
				&& this.getMinLoadIndex() < individual.getMinLoadIndex()) {
			return true;
		}

		// third objective equal
		// worst, worst, equal, best
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) < 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) < 0
				&& this.tip.compareTo(individual.tip) == 0
				&& this.getMinLoadIndex() > individual.getMinLoadIndex()) {
			return true;
		}
		// worst, best, equal, worst
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) < 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) > 0
				&& this.tip.compareTo(individual.tip) == 0
				&& this.getMinLoadIndex() < individual.getMinLoadIndex()) {
			return true;
		}
		// best, worst, equal, worst
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) > 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) < 0
				&& this.tip.compareTo(individual.tip) == 0
				&& this.getMinLoadIndex() < individual.getMinLoadIndex()) {
			return true;
		}

		// fourth objective equal
		// worst, worst, best, equal
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) < 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) < 0
				&& this.tip.compareTo(individual.tip) < 0
				&& this.getMinLoadIndex() == individual.getMinLoadIndex()) {
			return true;
		}
		// worst, best, worst, equal
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) < 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) > 0
				&& this.tip.compareTo(individual.tip) > 0
				&& this.getMinLoadIndex() == individual.getMinLoadIndex()) {
			return true;
		}
		// best, worst, worst, equal
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) > 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) < 0
				&& this.tip.compareTo(individual.tip) > 0
				&& this.getMinLoadIndex() == individual.getMinLoadIndex()) {
			return true;
		}

		/*
		 * equal in two objectives, best in one and worst in one (12)
		 */
		// 1th and 2th objective equal
		// equal, equal, best, worst
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) == 0
				&& this.tip.compareTo(individual.tip) < 0
				&& this.getMinLoadIndex() < individual.getMinLoadIndex()) {
			return true;
		}
		// equal, equal, worst, best
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) == 0
				&& this.tip.compareTo(individual.tip) > 0
				&& this.getMinLoadIndex() > individual.getMinLoadIndex()) {
			return true;
		}

		// 1th and 3th objective equal
		// equal, best, equal, worst
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) > 0
				&& this.tip.compareTo(individual.tip) == 0
				&& this.getMinLoadIndex() < individual.getMinLoadIndex()) {
			return true;
		}
		// equal, worst, equal, best
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) < 0
				&& this.tip.compareTo(individual.tip) == 0
				&& this.getMinLoadIndex() > individual.getMinLoadIndex()) {
			return true;
		}

		// 1th and 4th objective equal
		// equal, best, worst, equal
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) > 0
				&& this.tip.compareTo(individual.tip) > 0
				&& this.getMinLoadIndex() == individual.getMinLoadIndex()) {
			return true;
		}
		// equal, worst, best, equal
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) < 0
				&& this.tip.compareTo(individual.tip) < 0
				&& this.getMinLoadIndex() == individual.getMinLoadIndex()) {
			return true;
		}

		// 2th and 3th objective equal
		// best, equal, equal, worst
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) > 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) == 0
				&& this.tip.compareTo(individual.tip) == 0
				&& this.getMinLoadIndex() < individual.getMinLoadIndex()) {
			return true;
		}
		// worst, equal, equal, best
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) < 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) == 0
				&& this.tip.compareTo(individual.tip) == 0
				&& this.getMinLoadIndex() > individual.getMinLoadIndex()) {
			return true;
		}

		// 2th and 4th objective equal
		// best, equal, worst, equal
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) > 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) == 0
				&& this.tip.compareTo(individual.tip) > 0
				&& this.getMinLoadIndex() == individual.getMinLoadIndex()) {
			return true;
		}
		// worst, equal, best, equal
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) < 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) == 0
				&& this.tip.compareTo(individual.tip) < 0
				&& this.getMinLoadIndex() == individual.getMinLoadIndex()) {
			return true;
		}

		// 3th and 4th objective equal
		// best, worst, equal, equal
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) > 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) < 0
				&& this.tip.compareTo(individual.tip) == 0
				&& this.getMinLoadIndex() == individual.getMinLoadIndex()) {
			return true;
		}
		// worst, best, equal, equal
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) < 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) > 0
				&& this.tip.compareTo(individual.tip) == 0
				&& this.getMinLoadIndex() == individual.getMinLoadIndex()) {
			return true;
		}
		return false;
	}

	private boolean dominatedBy(Individual individual) {
		/*
		 * worst in all
		 */
		// worst, worst, worst, worst
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) < 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) < 0
				&& this.tip.compareTo(individual.tip) > 0
				&& this.getMinLoadIndex() < individual.getMinLoadIndex()) {
			return true;
		}

		/*
		 * worst in at least a objective and equal to other ones (4)
		 */
		// worst, equal, equal, equal
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) < 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) == 0
				&& this.tip.compareTo(individual.tip) == 0
				&& this.getMinLoadIndex() == individual.getMinLoadIndex()) {
			return true;
		}
		// equal, worst, equal, equal
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) < 0
				&& this.tip.compareTo(individual.tip) == 0
				&& this.getMinLoadIndex() == individual.getMinLoadIndex()) {
			return true;
		}

		// equal, equal, worst, equal
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) == 0
				&& this.tip.compareTo(individual.tip) > 0
				&& this.getMinLoadIndex() == individual.getMinLoadIndex()) {
			return true;
		}
		// equal, equal, equal, worst
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) == 0
				&& this.tip.compareTo(individual.tip) == 0
				&& this.getMinLoadIndex() < individual.getMinLoadIndex()) {
			return true;
		}

		/*
		 * worst in two objective and equals to others ones (6)
		 */
		// worst, worst, equal, equal
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) < 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) < 0
				&& this.tip.compareTo(individual.tip) == 0
				&& this.getMinLoadIndex() == individual.getMinLoadIndex()) {
			return true;
		}
		// worst, equal, worst, equal
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) < 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) == 0
				&& this.tip.compareTo(individual.tip) > 0
				&& this.getMinLoadIndex() == individual.getMinLoadIndex()) {
			return true;
		}
		// worst, equal, equal, worst
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) < 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) == 0
				&& this.tip.compareTo(individual.tip) == 0
				&& this.getMinLoadIndex() < individual.getMinLoadIndex()) {
			return true;
		}
		// equal, worst, worst, equal
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) < 0
				&& this.tip.compareTo(individual.tip) > 0
				&& this.getMinLoadIndex() == individual.getMinLoadIndex()) {
			return true;
		}
		// equal, worst, equal, worst
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) < 0
				&& this.tip.compareTo(individual.tip) == 0
				&& this.getMinLoadIndex() < individual.getMinLoadIndex()) {
			return true;
		}
		// equal, equal, worst, worst
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) == 0
				&& this.tip.compareTo(individual.tip) > 0
				&& this.getMinLoadIndex() < individual.getMinLoadIndex()) {
			return true;
		}

		/*
		 * worst in three objective and equals to others ones (4)
		 */
		// worst, worst, worst, equal
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) < 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) < 0
				&& this.tip.compareTo(individual.tip) > 0
				&& this.getMinLoadIndex() == individual.getMinLoadIndex()) {
			return true;
		}
		// worst, worst, equal, worst
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) < 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) < 0
				&& this.tip.compareTo(individual.tip) == 0
				&& this.getMinLoadIndex() < individual.getMinLoadIndex()) {
			return true;
		}
		// worst, equal, worst, worst
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) < 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) == 0
				&& this.tip.compareTo(individual.tip) > 0
				&& this.getMinLoadIndex() < individual.getMinLoadIndex()) {
			return true;
		}
		// equal, worst, worst, worst
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) < 0
				&& this.tip.compareTo(individual.tip) > 0
				&& this.getMinLoadIndex() < individual.getMinLoadIndex()) {
			return true;
		}
		return false;
	}

	public int crowdedComparison(Individual individual) {
		if (individual == null) {
			return 1;
		}
		// this greater than
		if (this.rank < individual.rank) {
			return 1;
		}
		if (this.rank == individual.rank
				&& crowdedDistance.compareTo(individual.crowdedDistance) > 0) {
			return 1;
		}
		if (this.rank == individual.rank
				&& crowdedDistance.compareTo(individual.crowdedDistance) == 0) {
			return 1;
		}
		// this less than
		if (this.rank > individual.rank) {
			return -1;
		}
		if (this.rank == individual.rank
				&& crowdedDistance.compareTo(individual.crowdedDistance) < 0) {
			return -1;
		}
		// equal
		return 0;
	}

	public int compareByTIC(Individual individual) {
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) > 0) {
			return 1;
		}
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) < 0) {
			return -1;
		}
		return 0;
	}

	public int compareByTIM(Individual individual) {
		if (this.timWithOverhead.compareTo(individual.timWithOverhead) > 0) {
			return 1;
		}
		if (this.timWithOverhead.compareTo(individual.timWithOverhead) < 0) {
			return -1;
		}
		return 0;
	}

	public int compareByTIP(Individual individual) {
		if (this.tip.compareTo(individual.tip) < 0) {
			return 1;
		}
		if (this.tip.compareTo(individual.tip) > 0) {
			return -1;
		}
		return 0;
	}

	public int compareByCrowdedDistance(Individual individual) {
		if (this.crowdedDistance.compareTo(individual.crowdedDistance) > 0) {
			return 1;
		}
		if (this.crowdedDistance.compareTo(individual.crowdedDistance) < 0) {
			return -1;
		}
		return 0;
	}

	public int compareByOriginDistance(Individual individual) {
		// current distance
		BigDecimal currentDistance = this.calculateOriginDistance(this);
		// other distance
		BigDecimal otherDistance = this.calculateOriginDistance(individual);
		if (currentDistance.compareTo(otherDistance) > 0) {
			return 1;
		}
		if (currentDistance.compareTo(otherDistance) < 0) {
			return -1;
		}
		return 0;
	}

	private BigDecimal calculateOriginDistance(Individual i) {
		BigDecimal distance = i.ticWithOverhead.add(i.timWithOverhead);
		BigDecimal price = DCSConstants.MAX_PRICE.subtract(i.tip);
		distance = distance.add(price);
		return distance;
	}

	public BigDecimal getTic() {
		return tic;
	}

	public void setTic(BigDecimal tic) {
		this.tic = tic;
	}

	public BigDecimal getTim() {
		return tim;
	}

	public void setTim(BigDecimal tim) {
		this.tim = tim;
	}

	@Override
	public boolean equals(Object o) {
		Individual individual = (Individual) o;
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 0
				&& this.timWithOverhead.compareTo(individual.timWithOverhead) == 0
				&& this.tip.compareTo(individual.tip) == 0
				&& this.getMinLoadIndex() == individual.getMinLoadIndex()) {
			return true;
		}
		return false;
	}

	public int compareByObjective(Individual individual) {
		int currentScore = 0;
		int compScore = 0;
		// comparing per objectives
		if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == 1) {
			currentScore++;
		} else if (this.ticWithOverhead.compareTo(individual.ticWithOverhead) == -1) {
			compScore++;
		}
		if (this.timWithOverhead.compareTo(individual.timWithOverhead) == 1) {
			currentScore++;
		} else if (this.timWithOverhead.compareTo(individual.timWithOverhead) == -1) {
			compScore++;
		}
		if (this.tip.compareTo(individual.tip) == -1) {
			currentScore++;
		} else if (this.tip.compareTo(individual.tip) == 1) {
			compScore++;
		}
		// if (this.getMinLoadIndex() > individual.getMinLoadIndex()) {
		// currentScore++;
		// } else if (this.getMinLoadIndex() < individual.getMinLoadIndex()) {
		// compScore++;
		// }
		//
		if (currentScore > compScore) {
			return 1;
		} else if (currentScore < compScore) {
			return -1;
		}
		return 0;
	}

}
