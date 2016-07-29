package broker.mo.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.apache.log4j.Logger;

import broker.mo.util.DCSConstants;
import broker.mo.util.ExtraInputs;

/**
 * 
 * @author Lino Chamorro
 * 
 */
public class CloudBrokerInstance {

	Logger log = Logger.getLogger(getClass());

	// **************//
	// *** INPUTS ***//
	// **************//
	/**
	 * k: provider Id. providers[k]: provider name
	 */
	String[] providers;
	/**
	 * j: instance Id. instanceNames[j]: instance name
	 */
	String[] instanceName;
	/**
	 * j: instance Id. instanceCores[j]: instance cpu cores
	 */
	BigDecimal[] instanceCores;
	/**
	 * j: instance Id. instanceMemory[j]: instance memory in gb
	 */
	BigDecimal[] instanceMemory;
	/**
	 * x: providers k. y: instances j
	 */
	BigDecimal[][] prices;
	/**
	 * Service downtime statistics x: providers k. y: instances j
	 */
	BigDecimal[][] downtimeStatistics;
	/**
	 * 0: VMs. 1 - TIPmax/TICmin. 2: LOCmin
	 */
	BigDecimal[] clientRequirements;

	// ********************//
	// *** EXTRA INPUTS ***//
	// ********************//
	ExtraInputs extraInputs;

	// **********************//
	// *** Aux structures ***//
	// **********************//
	/**
	 * Price per capacity unit
	 */
	BigDecimal[][] pricePerCapacity;
	/**
	 * Capacity per price unit
	 */
	BigDecimal[][] capacityPerPrice;
	/**
	 * Overhead index
	 */
	BigDecimal[][] overheadIndex;

	// **************//
	// *** OUTPUT ***//
	// **************//
	/**
	 * t-1 solution
	 */
	Individual prevSolution;

	/**
	 * Best solution
	 */
	List<Individual> paretoFront;
	Individual selectedSolution;

	/**
	 * indica si fue o no resuelto el problema
	 */
	boolean solutionFound;

	boolean init(String[] providers, String[] instanceName,
			BigDecimal[] instanceCores, BigDecimal[] instanceMemory,
			BigDecimal[][] prices, BigDecimal[][] downtimeStatistics,
			BigDecimal[] clientRequirements, ExtraInputs extraInputs) {
		this.providers = providers;
		this.instanceName = instanceName;
		this.instanceCores = instanceCores;
		this.instanceMemory = instanceMemory;
		this.prices = prices;
		this.downtimeStatistics = downtimeStatistics;
		this.clientRequirements = clientRequirements;
		this.extraInputs = extraInputs;
		if (!isValidLOC(extraInputs.loadBalanceMin)) {
			log.error("Invalid LOCmin: " + extraInputs.loadBalanceMin);
			return false;
		}
		return true;
	}

	/**
	 * Validar si el loc es factible considerando la cantidad de vms y
	 * proveedores
	 * 
	 * @param loc
	 * @return
	 */
	boolean isValidLOC(BigDecimal loc) {
		double n = extraInputs.vmToDeploy.doubleValue();
		int m = providers.length;
		double maxMinLOC = ((int) n / m) / n;
		double locDouble = loc.doubleValue();
		if (locDouble > maxMinLOC) {
			log.warn("Invalid LOCmin: " + locDouble);
			log.warn("Max LOCmin expected: " + maxMinLOC);
			return false;
		}
		return true;
	}

	boolean isValidCapacityThreshold(BigDecimal tip) {
		return false;
	}

	/**
	 * 
	 * @param tip
	 * @param loc
	 * @return
	 */
	boolean isValidBudget(BigDecimal tip, BigDecimal loc) {
		// TODO
		return false;
	}

	void printInput() {
		StringBuilder sb = new StringBuilder();
		sb.append("\n\n*** INPUT ***");
		// print providers
		sb.append(this.getProvidersToString());
		// print instances types
		sb.append(this.getInstanceTypesToString());
		// prices
		sb.append(this.getPricesToString());
		// downtime statistics
		sb.append(this.getDowntimeToString());
		// Requirements
		sb.append("\n-- Client requirements --");
		sb.append("\nVirtual Machine to deploy: " + extraInputs.vmToDeploy);
		sb.append("\ncpuMin: " + extraInputs.cpuMin);
		sb.append("\ncpuMax: " + extraInputs.cpuMax);
		sb.append("\nmemMin: " + extraInputs.memMin);
		sb.append("\nmemMax: " + extraInputs.memMax);
		sb.append("\nbudgetMin: " + extraInputs.budgetMin);
		sb.append("\nbudgetMax: " + extraInputs.budgetMax);
		sb.append("\nLOCmin: " + extraInputs.loadBalanceMin);
		sb.append("\nLOCmax: " + extraInputs.loadBalanceMax);
		sb.append("\nPopulations: " + extraInputs.population);
		sb.append("\nGenerations: " + extraInputs.generations);
		sb.append("\nSoftConstriants: " + extraInputs.softConstraints);
		sb.append("\nSelectionMode: " + extraInputs.selectionMode);
		sb.append("\n");
		log.info(sb.toString());
	}

	private String getProvidersToString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\n-- Providers --");
		sb.append("\nProvider ID\tName");
		for (int i = 0; i < providers.length; i++) {
			sb.append("\n\t" + i + "\t" + providers[i]);
		}
		sb.append("\n");
		return sb.toString();
	}

	private String getPricesToString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\n-- Prices --");
		sb.append("\nInstance ID");
		for (int i = 0; i < instanceName.length; i++) {
			sb.append("\t" + i);
		}
		for (int i = 0; i < prices.length; i++) {
			// provider id
			sb.append("\n\t" + i);
			for (int n = 0; n < prices[i].length; n++) {
				sb.append("\t" + prices[i][n]);
			}
		}
		sb.append("\n");
		return sb.toString();
	}

	private String getInstanceTypesToString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\n-- Instance Types --");
		sb.append("\nInstance Type");
		for (int i = 0; i < instanceName.length; i++) {
			sb.append("\t" + instanceName[i]);
		}
		sb.append("\nInstance ID");
		for (int i = 0; i < instanceName.length; i++) {
			sb.append("\t" + i);
		}
		sb.append("\nCores");
		for (int i = 0; i < instanceCores.length; i++) {
			sb.append("\t" + instanceCores[i]);
		}
		sb.append("\nMemory");
		for (int i = 0; i < instanceMemory.length; i++) {
			sb.append("\t" + instanceMemory[i]);
		}
		sb.append("\n");
		return sb.toString();
	}

	private String getDowntimeToString() {
		StringBuilder sb = new StringBuilder();
		sb.append("\n-- Downtime statistics --");
		sb.append("\nInstance Type");
		for (int i = 0; i < instanceName.length; i++) {
			sb.append("\t" + instanceName[i]);
		}
		for (int i = 0; i < providers.length; i++) {
			sb.append("\n" + providers[i]);
			sb.append("\talloc");
			for (int j = 0; j < instanceName.length; j++) {
				sb.append("\t" + downtimeStatistics[i * 2][j]);
			}
			sb.append("\n" + providers[i]);
			sb.append("\trelease");
			for (int j = 0; j < instanceName.length; j++) {
				sb.append("\t" + downtimeStatistics[i * 2 + 1][j]);
			}
		}
		sb.append("\n");
		return sb.toString();
	}

	@SuppressWarnings("unused")
	private String getMigrationOverheadCoreToString(Individual solution) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n-- Migration Overhead --");
		int n = solution.n;
		for (int i = 0; i < n; i++) {
			sb.append("\nVM: " + i + "\toverheadCore: "
					+ solution.migrationOverheadCore[i]);
		}
		sb.append("\n");
		return sb.toString();
	}

	@SuppressWarnings("unused")
	private String getMigrationTimeToString(Individual solution) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n-- Migration Time --");
		int n = solution.n;
		for (int i = 0; i < n; i++) {
			sb.append("\nVM: " + i + "\tmigrationTime: "
					+ solution.migrationTime[i]);
		}
		sb.append("\n");
		return sb.toString();
	}

	List<Individual> getParetoFront() {
		return paretoFront;
	}

	void printOutput() {
		if (!solutionFound) {
			log.info("\n\n*** NO SOLUTION FOUND ***\n");
			return;
		}
		log.info("\n\n*** OUTPUT ***");
		int solutionId = 0;
		for (Individual individual : paretoFront) {
			this.printOutput(individual, solutionId++);
		}
		log.info("\n**************");
	}

	void printOutput(Individual solution, int solutionId) {
		StringBuilder sb = new StringBuilder();
		sb.append(placementToString(solution));
		sb.append(instanceSumaryToString(solution));
		sb.append(instancePerProviderSumary(solution));
		if (solution.extraInputs != null) {
			if (solution.extraInputs.printOverhead
					&& solution.prevSolution != null) {
				sb.append("\n--MIGRATIONS PARAMS --\n");
				sb.append("\tMCCore:\t\t" + solution.mcCore);
				sb.append("\tMCMemory:\t\t" + solution.mcMemory);
				sb.append("\n-- TIC OVERHEAD --\n");
				sb.append("\tTIC OVERHEAD: " + solution.ticWithOverhead);
			}
		}
		sb.append("\n-- TIC --\n");
		sb.append("\tTIC: " + solution.tic);
		sb.append("\n-- TIM --\n");
		sb.append("\tTIM: " + solution.tim);
		sb.append("\n-- TIP --\n");
		sb.append("\tTIP: " + solution.tip);
		sb.append("\n-- LOC --\n\t");
		int k = solution.providers.length;
		for (int i = 0; i < k; i++) {
			sb.append(solution.providers[i] + "\t");
		}
		sb.append("\n\t");
		for (int i = 0; i < k; i++) {
			sb.append(solution.loc[i] + "\t");
		}
		sb.append("\n");
		log.info(sb.toString());
	}

	String getCSVPopulationGenerationTicTip(Individual solution) {
		return solution.extraInputs.population + DCSConstants.SEPARATOR
				+ solution.extraInputs.generations + DCSConstants.SEPARATOR
				+ solution.tic + DCSConstants.SEPARATOR
				+ solution.ticWithOverhead + DCSConstants.SEPARATOR
				+ solution.tim + DCSConstants.SEPARATOR
				+ solution.timWithOverhead + DCSConstants.SEPARATOR
				+ solution.tip + DCSConstants.SEPARATOR
				+ solution.getMinLoadIndex() + DCSConstants.SEPARATOR
				+ solution.crowdedDistance;
	}

	String getCSVLoc(Individual solution) {
		StringBuilder sb = new StringBuilder();
		int k = solution.providers.length;
		for (int i = 0; i < k; i++) {
			sb.append(solution.loc[i]);
			if (i - 1 != k) {
				sb.append(DCSConstants.SEPARATOR);
			}
		}
		return sb.toString();
	}

	String getCSVInstancePerProviderSummary(int r, int t, int solutionNro,
			Individual solution) {
		StringBuilder sb = new StringBuilder();
		int m = solution.providers.length;
		int l = solution.instanceName.length;
		boolean isMigTimeNull = solution.migrationTime == null;
		boolean isMigOverTicNull = solution.migrationOverheadCore == null;
		boolean isMigOverTimNull = solution.migrationOverheadMemory == null;
		for (int i = 0; i < m; i++) {
			for (int j = 0; j < l; j++) {
				BigDecimal price = solution.prices[i][j];
				BigDecimal capacity = solution.instanceCores[j];
				BigDecimal summary = solution.instancePerProviderSummary[i][j][0];
				int minLoadIndex = solution.getMinLoadIndex();
				if (summary == null) {
					summary = new BigDecimal(0);
				}
				BigDecimal migTime = new BigDecimal(0);
				BigDecimal migOverheadTic = new BigDecimal(0);
				BigDecimal migOverheadTim = new BigDecimal(0);
				if (!isMigTimeNull && summary.intValue() > 0) {
					migTime = solution.instancePerProviderSummary[i][j][1];
				}
				if (!isMigOverTicNull && summary.intValue() > 0) {
					migOverheadTic = solution.instancePerProviderSummary[i][j][2];

				}
				if (!isMigOverTimNull && summary.intValue() > 0) {
					migOverheadTim = solution.instancePerProviderSummary[i][j][3];

				}
				sb.append(r + DCSConstants.SEPARATOR + t
						+ DCSConstants.SEPARATOR + solutionNro
						+ DCSConstants.SEPARATOR
						+ solution.extraInputs.generations
						+ DCSConstants.SEPARATOR
						+ solution.extraInputs.population
						+ DCSConstants.SEPARATOR + solution.providers[i]
						+ DCSConstants.SEPARATOR + solution.instanceName[j]
						+ DCSConstants.SEPARATOR + capacity
						+ DCSConstants.SEPARATOR + price
						+ DCSConstants.SEPARATOR + summary
						+ DCSConstants.SEPARATOR + capacity.multiply(summary)
						+ DCSConstants.SEPARATOR + migTime
						+ DCSConstants.SEPARATOR + migOverheadTic
						+ DCSConstants.SEPARATOR + migOverheadTim
						+ DCSConstants.SEPARATOR + price.multiply(summary)
						+ DCSConstants.SEPARATOR + minLoadIndex
						+ DCSConstants.SEPARATOR + solution.crowdedDistance);
				if (j < (l - 1)) {
					sb.append("\n");
				}
			}
			if (i < (m - 1)) {
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	String placementToString(Individual solution) {
		int n = solution.n;
		StringBuilder sb = new StringBuilder();
		sb.append("\n-- Placement --");
		sb.append("\nVMi");
		for (int i = 0; i < n; i++) {
			sb.append("\t" + i);
		}
		for (int i = 0; i < 2; i++) {
			if (i == 0) {
				sb.append("\nITj");
			} else {
				sb.append("\nCPk");
			}
			for (int k = 0; k < n; k++) {
				sb.append("\t" + solution.output[i][k]);
			}
		}
		sb.append("\n");
		return sb.toString();
	}

	private String instanceSumaryToString(Individual solution) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n-- Instances Summary --");
		for (int i = 0; i < solution.instanceSummary.length; i++) {
			sb.append("\n\tInstance Type: " + i + " "
					+ solution.instanceName[i] + ", qty: "
					+ solution.instanceSummary[i]);
		}
		sb.append("\n");
		return sb.toString();
	}

	private String instancePerProviderSumary(Individual solution) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n-- Instances per Provider Summary --");
		for (int i = 0; i < solution.providers.length; i++) {
			sb.append("\n" + solution.providers[i]);
			for (int j = 0; j < solution.instanceName.length; j++) {
				int qty = solution.instancePerProviderSummary[i][j][0] != null ? solution.instancePerProviderSummary[i][j][0]
						.intValue() : 0;
				sb.append("\n\tInstance Type: " + j + " "
						+ solution.instanceName[j] + ", qty: " + qty);
			}
		}
		sb.append("\n");
		return sb.toString();
	}

	void printPricePerCapacity() {
		int j;
		int k;
		BigDecimal index;
		StringBuilder sb = new StringBuilder();
		sb.append("\n\n-- Price per capacity --");
		sb.append("\nIndex\tInstance Type\tCapacity\tProvider\tPrice");
		for (int i = 0; i < pricePerCapacity.length; i++) {
			index = pricePerCapacity[i][0].setScale(3,
					BigDecimal.ROUND_HALF_EVEN);
			j = pricePerCapacity[i][1].intValue();
			sb.append("\n" + index.doubleValue() + "\t");
			sb.append(j + ". " + instanceName[j] + "\t");
			sb.append(pricePerCapacity[i][2] + "\t");
			k = pricePerCapacity[i][3].intValue();
			sb.append(k + ". " + providers[k] + "\t");
			sb.append(pricePerCapacity[i][4] + "\t");
		}
		sb.append("\n");
		log.info(sb.toString());
	}

	BigDecimal getTIC(BigDecimal[][] output) {
		BigDecimal tic = new BigDecimal(0);
		BigDecimal capacity;
		int n = extraInputs.vmToDeploy.intValue();
		int j;
		for (int i = 0; i < n; i++) {
			j = output[0][i].intValue();
			capacity = instanceCores[j];
			tic = tic.add(capacity);
		}
		return tic;
	}

	BigDecimal getTIM(BigDecimal[][] output) {
		BigDecimal tim = new BigDecimal(0);
		BigDecimal capacity;
		int n = extraInputs.vmToDeploy.intValue();
		int j;
		for (int i = 0; i < n; i++) {
			j = output[0][i].intValue();
			capacity = instanceMemory[j];
			tim = tim.add(capacity);
		}
		return tim;
	}

	/**
	 * suma el overhead de cada vi seleccionado de un individuo y retorna el MC
	 * Core
	 * 
	 * @param output
	 * @return
	 */
	BigDecimal getMCCore(Individual individual) {
		BigDecimal[] migrationOverhead = individual.migrationOverheadCore;
		BigDecimal mc = new BigDecimal(0);
		for (BigDecimal deltai : migrationOverhead) {
			mc = mc.add(deltai);
		}
		return mc;
	}

	/**
	 * suma el overhead de cada vi seleccionado de un individuo y retorna el MC
	 * Memory
	 * 
	 * @param output
	 * @return
	 */
	BigDecimal getMCMemory(Individual individual) {
		BigDecimal[] migrationOverhead = individual.migrationOverheadMemory;
		BigDecimal mc = new BigDecimal(0);
		for (BigDecimal deltai : migrationOverhead) {
			mc = mc.add(deltai);
		}
		return mc;
	}

	BigDecimal getTIP(BigDecimal[][] output) {
		BigDecimal tip = new BigDecimal(0);
		BigDecimal price;
		int n = extraInputs.vmToDeploy.intValue();
		int j;
		int k;
		for (int i = 0; i < n; i++) {
			j = output[0][i].intValue();
			k = output[1][i].intValue();
			price = prices[k][j];
			tip = tip.add(price);
		}
		return tip;
	}

	BigDecimal[] getLOC(BigDecimal[][] output) {
		int m = providers.length;
		int k;
		BigDecimal[] loc = new BigDecimal[m];
		for (int i = 0; i < m; i++) {
			loc[i] = new BigDecimal(0);
		}
		int n = extraInputs.vmToDeploy.intValue();
		for (int i = 0; i < n; i++) {
			k = output[1][i].intValue();
			loc[k] = loc[k].add(new BigDecimal(1));
		}
		return loc;
	}

	int[] getInstanceSummary(Individual solution) {
		int n = solution.n;
		int[] summary = new int[solution.instanceName.length];
		for (int k = 0; k < n; k++) {
			summary[solution.output[0][k].intValue()] += 1;
		}
		return summary;
	}

	BigDecimal[][][] getInstancePerProviderSummary(Individual solution) {
		int n = solution.n;
		BigDecimal[][][] providerSummary = new BigDecimal[solution.providers.length][solution.instanceName.length][4];
		for (int i = 0; i < n; i++) {
			// instance qty count
			BigDecimal currentQty = providerSummary[solution.output[1][i]
					.intValue()][solution.output[0][i].intValue()][0];
			if (currentQty == null) {
				currentQty = new BigDecimal(0);
			}
			providerSummary[solution.output[1][i].intValue()][solution.output[0][i]
					.intValue()][0] = currentQty.add(new BigDecimal(1));
			// validar que t > 0
			if (solution.migrationTime != null
					&& solution.migrationOverheadCore != null) {
				// migration time count
				BigDecimal currentMigtime = providerSummary[solution.output[1][i]
						.intValue()][solution.output[0][i].intValue()][1];
				if (currentMigtime == null) {
					currentMigtime = new BigDecimal(0);
				}
				providerSummary[solution.output[1][i].intValue()][solution.output[0][i]
						.intValue()][1] = currentMigtime
						.add(solution.migrationTime[i]);
				// migration overhead core count
				BigDecimal currentOverheadCore = providerSummary[solution.output[1][i]
						.intValue()][solution.output[0][i].intValue()][2];
				if (currentOverheadCore == null) {
					currentOverheadCore = new BigDecimal(0);
				}
				providerSummary[solution.output[1][i].intValue()][solution.output[0][i]
						.intValue()][2] = currentOverheadCore
						.add(solution.migrationOverheadCore[i]);
				// migration overhead memory count
				BigDecimal currentOverheadMemory = providerSummary[solution.output[1][i]
						.intValue()][solution.output[0][i].intValue()][3];
				if (currentOverheadMemory == null) {
					currentOverheadMemory = new BigDecimal(0);
				}
				providerSummary[solution.output[1][i].intValue()][solution.output[0][i]
						.intValue()][3] = currentOverheadMemory
						.add(solution.migrationOverheadMemory[i]);
			}
		}
		return providerSummary;
	}

	// @Deprecated
	// void setPricePerCapacity() {
	// int l = instanceName.length;
	// int m = providers.length;
	// /*
	// * Se emplea una estructura auxiliar y luego se depurara
	// */
	// BigDecimal[][] pricePerCapacityTmp = new BigDecimal[l * m][5];
	// BigDecimal index;
	// int i = 0;
	// int count = 0;
	// for (int k = 0; k < m; k++) {
	// for (int j = 0; j < l; j++) {
	// if (prices[k][j].doubleValue() > 0) {
	// index = prices[k][j].divide(instanceCapacity[j]);
	// pricePerCapacityTmp[i][0] = index;
	// pricePerCapacityTmp[i][1] = new BigDecimal(j);
	// pricePerCapacityTmp[i][2] = instanceCapacity[j];
	// pricePerCapacityTmp[i][3] = new BigDecimal(k);
	// pricePerCapacityTmp[i][4] = prices[k][j];
	// count++;
	// } else {
	// /*
	// * Si la instancia j no se puede ejecutar en el proveedor k
	// * (N/A) se establece -1
	// */
	// pricePerCapacityTmp[i][0] = new BigDecimal(-1);
	// }
	// i++;
	// }
	// }
	// /*
	// * Matriz depurada
	// */
	// pricePerCapacity = new BigDecimal[count][5];
	// i = 0;
	// for (int j = 0; j < l * m; j++) {
	// index = pricePerCapacityTmp[j][0];
	// if (index.doubleValue() > 0) {
	// pricePerCapacity[i][0] = pricePerCapacityTmp[j][0];
	// pricePerCapacity[i][1] = pricePerCapacityTmp[j][1];
	// pricePerCapacity[i][2] = pricePerCapacityTmp[j][2];
	// pricePerCapacity[i][3] = pricePerCapacityTmp[j][3];
	// pricePerCapacity[i][4] = pricePerCapacityTmp[j][4];
	// i++;
	// }
	// }
	// QuickSort quickSort = new QuickSort();
	// quickSort.sort(pricePerCapacity, extraInputs.optimizeTIC,
	// extraInputs.optimizeTIP, extraInputs.optimizeMC);
	// }

	// @Deprecated
	// void setCapacityPerPrice() {
	// int l = instanceName.length;
	// int m = providers.length;
	// /*
	// * Se emplea una estructura auxiliar y luego se depurara
	// */
	// BigDecimal[][] capacityPerPriceTmp = new BigDecimal[l * m][5];
	// BigDecimal index;
	// int i = 0;
	// int count = 0;
	// for (int k = 0; k < m; k++) {
	// for (int j = 0; j < l; j++) {
	// if (prices[k][j].doubleValue() > 0) {
	// index = instanceCapacity[j].divide(prices[k][j], 4,
	// RoundingMode.HALF_EVEN);
	// capacityPerPriceTmp[i][0] = index;
	// capacityPerPriceTmp[i][1] = new BigDecimal(j);
	// capacityPerPriceTmp[i][2] = instanceCapacity[j];
	// capacityPerPriceTmp[i][3] = new BigDecimal(k);
	// capacityPerPriceTmp[i][4] = prices[k][j];
	// count++;
	// } else {
	// /*
	// * Si la instancia j no se puede ejecutar en el proveedor k
	// * (N/A) se establece -1
	// */
	// capacityPerPriceTmp[i][0] = new BigDecimal(-1);
	// }
	// i++;
	// }
	// }
	// /*
	// * Matriz depurada
	// */
	// capacityPerPrice = new BigDecimal[count][5];
	// i = 0;
	// for (int j = 0; j < l * m; j++) {
	// index = capacityPerPriceTmp[j][0];
	// if (index.doubleValue() > 0) {
	// capacityPerPrice[i][0] = capacityPerPriceTmp[j][0];
	// capacityPerPrice[i][1] = capacityPerPriceTmp[j][1];
	// capacityPerPrice[i][2] = capacityPerPriceTmp[j][2];
	// capacityPerPrice[i][3] = capacityPerPriceTmp[j][3];
	// capacityPerPrice[i][4] = capacityPerPriceTmp[j][4];
	// i++;
	// }
	// }
	// QuickSort quickSort = new QuickSort();
	// quickSort.sort(capacityPerPrice, extraInputs.optimizeTIC,
	// extraInputs.optimizeTIP, extraInputs.optimizeMC);
	// }

	// @Deprecated
	// void setOverheadIndex() {
	// int l = instanceName.length;
	// int m = providers.length;
	// /*
	// * Se emplea una estructura auxiliar y luego se depurara
	// */
	// BigDecimal[][] overheadIndexTmp = new BigDecimal[l * m][5];
	// BigDecimal releaseTimeVMi;
	// BigDecimal delta;
	// int i = 0;
	// int count = 0;
	// for (int k = 0; k < m; k++) {
	// for (int j = 0; j < l; j++) {
	// if (prices[k][j].doubleValue() > 0) {
	// releaseTimeVMi = downtimeStatistics[k + 1][j];
	// delta = this.getDelta(-1, j, k, releaseTimeVMi);
	// overheadIndexTmp[i][0] = delta;
	// overheadIndexTmp[i][1] = new BigDecimal(j);
	// overheadIndexTmp[i][2] = instanceCapacity[j];
	// overheadIndexTmp[i][3] = new BigDecimal(k);
	// overheadIndexTmp[i][4] = prices[k][j];
	// count++;
	// } else {
	// /*
	// * Si la instancia j no se puede ejecutar en el proveedor k
	// * (N/A) se establece -1
	// */
	// overheadIndexTmp[i][0] = new BigDecimal(-1);
	// }
	// i++;
	// }
	// }
	// /*
	// * Matriz depurada
	// */
	// overheadIndex = new BigDecimal[count][5];
	// i = 0;
	// for (int j = 0; j < l * m; j++) {
	// delta = overheadIndexTmp[j][0];
	// if (delta.doubleValue() > 0) {
	// overheadIndex[i][0] = overheadIndexTmp[j][0];
	// overheadIndex[i][1] = overheadIndexTmp[j][1];
	// overheadIndex[i][2] = overheadIndexTmp[j][2];
	// overheadIndex[i][3] = overheadIndexTmp[j][3];
	// overheadIndex[i][4] = overheadIndexTmp[j][4];
	// i++;
	// }
	// }
	// QuickSort quickSort = new QuickSort();
	// quickSort.sort(capacityPerPrice, extraInputs.optimizeTIC,
	// extraInputs.optimizeTIP, extraInputs.optimizeMC);
	// }

	/**
	 * get migration overhead cores from output
	 * 
	 * @param output
	 * @return
	 */
	BigDecimal[] getMigrationOverhead(Individual individual, boolean core,
			boolean memory) {
		BigDecimal[][] output = individual.output;
		int n = extraInputs.vmToDeploy.intValue();
		BigDecimal[] migrationOverhead = new BigDecimal[n];
		for (int i = 0; i < n; i++) {
			migrationOverhead[i] = new BigDecimal(0);
		}
		// BigDecimal releaseTimeVMi;
		BigDecimal delta;
		BigDecimal beta = null;
		BigDecimal mc;
		int j;
		// int k;
		for (int i = 0; i < n; i++) {
			/*
			 * Instance type for VMi
			 */
			j = output[0][i].intValue();
			/*
			 * Provider for VMi
			 */
			// k = output[1][i].intValue();
			/*
			 * capacity of j'
			 */
			if (core) {
				beta = instanceCores[j];
			} else if (memory) {
				beta = instanceMemory[j];
			}
			delta = individual.migrationTime[i];
			mc = beta.multiply(delta);
			BigDecimal hourInSeconds = new BigDecimal(3600);
			mc = mc.divide(hourInSeconds, 3, RoundingMode.HALF_EVEN);
			migrationOverhead[i] = mc;
		}
		return migrationOverhead;
	}

	/**
	 * Calcula delta i: suma de release y alloc
	 * 
	 * @param VMi
	 *            no se usa en el calculo de overhead
	 * @param currentJ
	 * @param currentK
	 * @param releaseTime
	 * @return
	 */
	BigDecimal getDelta(int i, int currentJ, int currentK,
			BigDecimal releaseTime) {
		BigDecimal overhead = new BigDecimal(0);
		for (int k = 0; k < providers.length; k++) {
			BigDecimal allocTime;
			for (int j = 0; j < instanceName.length; j++) {
				/*
				 * el overhead se calcula para un tipo de instancia y proveedor
				 * distinto al actual
				 */
				if (j != currentJ || k != currentK) {
					allocTime = downtimeStatistics[(k * 2)][j];
					if (allocTime.intValue() >= 0) {
						overhead = overhead.add(allocTime.add(releaseTime));
					}
				}
			}
		}
		// BigDecimal hourInSeconds = new BigDecimal(3600);
		// overhead = overhead.divide(hourInSeconds, 3, RoundingMode.HALF_EVEN);
		return overhead;
	}

	BigDecimal getDeltaReloaded(int i, int currentJ, int currentK,
			BigDecimal releaseTime) {
		BigDecimal overhead = new BigDecimal(0);

		/*
		 * el overhead se calcula para un tipo de instancia y proveedor distinto
		 * al actual
		 */
		BigDecimal allocTime = downtimeStatistics[(currentK * 2)][currentJ];
		overhead = allocTime.add(releaseTime);
		BigDecimal hourInSeconds = new BigDecimal(3600);
		overhead = overhead.divide(hourInSeconds, 3, RoundingMode.HALF_EVEN);
		return overhead;
	}

	BigDecimal[] getMigrationTime(Individual individual) {
		BigDecimal[][] outputPrev = this.prevSolution.output;
		BigDecimal[][] output = individual.output;
		int n = extraInputs.vmToDeploy.intValue();
		BigDecimal[] migrationTime = new BigDecimal[n];
		BigDecimal releaseTimeVMi;
		BigDecimal allocationTimeVMi;
		int prevJ = -1;
		int prevK = -1;
		int j;
		int k;
		for (int i = 0; i < n; i++) {
			// PREV ALLOCATION
			if (i < this.prevSolution.n) {
				/*
				 * Instance type for VMi
				 */
				prevJ = outputPrev[0][i].intValue();
				/*
				 * Provider for VMi
				 */
				prevK = outputPrev[1][i].intValue();
				releaseTimeVMi = prevSolution.downtimeStatistics[(prevK * 2) + 1][prevJ];
				log.trace("VMi=" + i + ",prevJ=" + prevJ + ",prevK=" + prevK
						+ ", releaseTime=" + releaseTimeVMi);
			} else {
				releaseTimeVMi = new BigDecimal(0);
			}
			// CURRENT ALLOCATION
			/*
			 * Instance type for VMi
			 */
			j = output[0][i].intValue();
			/*
			 * Provider for VMi
			 */
			k = output[1][i].intValue();
			allocationTimeVMi = downtimeStatistics[(k * 2)][j];
			log.trace("VMi=" + i + ",j=" + j + ",k=" + k
					+ ", allocationTimeVMi=" + allocationTimeVMi);
			if (prevJ != j || prevK != k) {
				migrationTime[i] = allocationTimeVMi.add(releaseTimeVMi);
			} else {
				migrationTime[i] = new BigDecimal(0);
			}
		}
		return migrationTime;
	}

}
