package broker.mo.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * 
 * @author Lino Chamorro
 * 
 */
public class LoadInputs {

	final Logger log = Logger.getLogger(getClass());

	List<CloudProvider> cloudProviders;
	List<InstanceType> instanceTypes;
	List<MigrationStatistics> migrationStatistics;
	ExtraInputs extraInputs;

	public void processInput(String filePath) {
		List<String> file = this.readFile(filePath);
		// load providers
		cloudProviders = loadCloudProviders(file);
		// load instances types
		instanceTypes = loadInstanceTypesAndPrices(file, cloudProviders);
		// load overhead
		migrationStatistics = loadMigrationOverhead(file, cloudProviders,
				instanceTypes);
		// load aditional inputs
		extraInputs = loadAditionalInputs(file);
	}

	private List<CloudProvider> loadCloudProviders(List<String> file) {
		log.debug("Loading providers..");
		List<CloudProvider> providers = new ArrayList<CloudProvider>();
		CloudProvider cp;
		// k index for cloud, begin in 0
		int index = 0;
		for (String line : file) {
			if (line.trim().contains(DCSConstants.PROVIDERS_BEGIN)) {
				continue;
			}
			if (line.trim().contains(DCSConstants.PROVIDERS_END)) {
				break;
			}
			if (line != null && line.trim().length() > 0) {
				cp = new CloudProvider(index++, line.trim());
				log.debug("Adding " + cp);
				providers.add(cp);
			}
		}
		log.debug(providers.size() + " providers loaded!");
		return providers;
	}

	private List<InstanceType> loadInstanceTypesAndPrices(List<String> file,
			List<CloudProvider> cloudProviders) {
		log.debug("Loading instancesTypes..");
		int instanceIndex = 0;
		List<InstanceType> instanceTypeList = new ArrayList<InstanceType>();
		InstanceType instanceType;
		String[] split;
		CloudProvider provider;
		int providerIndex = 0;
		boolean readingInstances = false;
		boolean readingPrices = false;
		for (String line : file) {
			if (line.trim().contains(DCSConstants.INSTANCE_TYPES_BEGIN)) {
				readingInstances = true;
				continue;
			}
			if (line.trim().contains(DCSConstants.INSTANCE_TYPES_END)) {
				break;
			}
			if (line != null && line.trim().length() > 0 && readingInstances) {
				split = line.split("\\s+");
				if (split.length < 2) {
					continue;
				}
				// load labels
				if (split[1].trim().contains(DCSConstants.LABEL)) {
					for (int i = 2; i < split.length; i++) {
						instanceType = new InstanceType(instanceIndex++,
								split[i]);
						instanceTypeList.add(instanceType);
					}
					continue;
				}
				// load cores
				if (split[1].trim().contains(DCSConstants.CORES)) {
					for (int i = 2; i < split.length; i++) {
						instanceType = instanceTypeList.get(i - 2);
						instanceType.cores = new BigDecimal(split[i]);
						log.debug("Adding " + instanceType);
					}
					continue;
				}
				// load memory
				if (split[1].trim().contains(DCSConstants.MEMORY)) {
					for (int i = 2; i < split.length; i++) {
						instanceType = instanceTypeList.get(i - 2);
						instanceType.memory = new BigDecimal(split[i]);
						log.debug("Setting memory: " + instanceType);
					}
					continue;
				}
				// load prices per cloud
				if (line.trim().contains(DCSConstants.PRICES)) {
					readingPrices = true;
					continue;
				}
				if (readingPrices) {
					String providerName = split[1];
					provider = cloudProviders.get(providerIndex++);
					if (!providerName.equals(provider.name)) {
						log.error("Provider expected: " + provider.name
								+ ", provider found: " + providerName);
						return null;
					}
					for (int i = 2; i < split.length; i++) {
						instanceType = instanceTypeList.get(i - 2);
						if (!split[i].contains(DCSConstants.NOT_APPLICABLE)) {
							BigDecimal price = new BigDecimal(split[i].trim());
							log.debug("Price: " + price + ", " + instanceType
									+ ", " + provider);
							instanceType.cloudAndPrices.put(provider, price);
						} else {
							log.warn(instanceType + " N/A for provider "
									+ provider);
						}
					}
				}
			}
		}
		log.debug(instanceTypeList.size() + " instances types loaded!");
		return instanceTypeList;
	}

	private List<MigrationStatistics> loadMigrationOverhead(List<String> file,
			List<CloudProvider> cloudProviders, List<InstanceType> instanceTypes) {
		log.debug("Loading migration overhead..");
		List<MigrationStatistics> overheadList = new ArrayList<MigrationStatistics>();
		MigrationStatistics migrationOverhead;
		InstanceType instanceType;
		CloudProvider provider;
		boolean readingOverhead = false;
		String[] split;
		Iterator<String> it = file.iterator();
		String line;
		String[] allocLine;
		String[] deAllocLine;
		int providerIndex = 0;
		while (it.hasNext()) {
			line = it.next();
			if (line.trim().contains(DCSConstants.OVERHEAD_BEGIN)) {
				readingOverhead = true;
				continue;
			}
			if (line.trim().contains(DCSConstants.OVERHEAD_END)) {
				break;
			}
			if (line != null && line.trim().length() > 0 && readingOverhead) {
				split = line.split("\\s+");
				if (split.length < 2) {
					continue;
				}
				// labels
				if (split[1].trim().contains(DCSConstants.LABEL)) {
					// validate IT position
				}
				// overhead by cloud provider
				if (split.length == 2) {
					String providerName = split[1];
					provider = cloudProviders.get(providerIndex++);
					if (!providerName.equals(provider.name)) {
						log.error("Provider expected: " + provider.name
								+ ", provider found: " + providerName);
						return null;
					}
					allocLine = it.next().split("\\s+");
					deAllocLine = it.next().split("\\s+");
					for (int i = 2; i < allocLine.length; i++) {
						instanceType = instanceTypes.get(i - 2);
						if (!allocLine[i].contains(DCSConstants.NOT_APPLICABLE)) {
							migrationOverhead = new MigrationStatistics();
							migrationOverhead.allocation = new BigDecimal(
									allocLine[i]);
							migrationOverhead.deAllocation = new BigDecimal(
									deAllocLine[i]);
							migrationOverhead.cloudProvider = provider;
							migrationOverhead.instanceType = instanceType;
							log.debug(migrationOverhead);
							overheadList.add(migrationOverhead);
							instanceType.migrationOverhead.put(provider,
									migrationOverhead);
						} else {
							log.warn(instanceType
									+ " overhead N/A for provider " + provider);
						}
					}
				}
			}
		}
		log.debug(overheadList.size() + " migration overhead added!");
		return overheadList;
	}

	private ExtraInputs loadAditionalInputs(List<String> file) {
		log.debug("Loading constraints..");
		int index = 0;
		boolean readingConstraints = false;
		ExtraInputs extraInputs = new ExtraInputs();
		for (String line : file) {
			if (line.trim().contains(DCSConstants.RESTRICTION_BEGIN)) {
				readingConstraints = true;
				continue;
			}
			if (line.trim().contains(DCSConstants.RESTRICTION_END)) {
				break;
			}
			if (!readingConstraints) {
				continue;
			}
			String[] splitAux = line.split("\\s+");
			if (splitAux.length > 1) {
				String[] split = splitAux[1].split("=");
				if (split[0].contains(DCSConstants.VM_TO_DEPLOY)) {
					extraInputs.vmToDeploy = new BigDecimal(split[1]);
					log.debug(DCSConstants.VM_TO_DEPLOY + ": "
							+ extraInputs.vmToDeploy);
					index++;
				}
				if (split[0].contains(DCSConstants.CPU_MIN)) {
					extraInputs.cpuMin = new BigDecimal(split[1]);
					log.debug(DCSConstants.CPU_MIN + ": " + extraInputs.cpuMin);
					index++;
				}
				if (split[0].contains(DCSConstants.CPU_MAX)) {
					extraInputs.cpuMax = new BigDecimal(split[1]);
					log.debug(DCSConstants.CPU_MAX + ": " + extraInputs.cpuMax);
					index++;
				}
				if (split[0].contains(DCSConstants.CPU_EXPECTED)) {
					extraInputs.cpuExpected = new BigDecimal(split[1]);
					log.debug(DCSConstants.CPU_EXPECTED + ": "
							+ extraInputs.cpuExpected);
					index++;
				}
				if (split[0].contains(DCSConstants.MEM_MIN)) {
					extraInputs.memMin = new BigDecimal(split[1]);
					log.debug(DCSConstants.MEM_MIN + ": " + extraInputs.memMin);
					index++;
				}
				if (split[0].contains(DCSConstants.MEM_MAX)) {
					extraInputs.memMax = new BigDecimal(split[1]);
					log.debug(DCSConstants.MEM_MAX + ": " + extraInputs.memMax);
					index++;
				}
				if (split[0].contains(DCSConstants.MEM_EXPECTED)) {
					extraInputs.memExpected = new BigDecimal(split[1]);
					log.debug(DCSConstants.MEM_EXPECTED + ": "
							+ extraInputs.memExpected);
					index++;
				}
				if (split[0].contains(DCSConstants.BUDGET_MIN)) {
					extraInputs.budgetMin = new BigDecimal(split[1]);
					log.debug(DCSConstants.BUDGET_MIN + ": "
							+ extraInputs.budgetMin);
					index++;
				}
				if (split[0].contains(DCSConstants.BUDGET_MAX)) {
					extraInputs.budgetMax = new BigDecimal(split[1]);
					log.debug(DCSConstants.BUDGET_MAX + ": "
							+ extraInputs.budgetMax);
					index++;
				}
				if (split[0].contains(DCSConstants.BUDGET_EXPECTED)) {
					extraInputs.budgetExpected = new BigDecimal(split[1]);
					log.debug(DCSConstants.BUDGET_EXPECTED + ": "
							+ extraInputs.budgetExpected);
					index++;
				}
				if (split[0].contains(DCSConstants.LOAD_BALANCE_MIN)) {
					extraInputs.loadBalanceMin = new BigDecimal(split[1]);
					log.debug(DCSConstants.LOAD_BALANCE_MIN + ": "
							+ extraInputs.loadBalanceMin);
					index++;
				}
				if (split[0].contains(DCSConstants.LOAD_BALANCE_MAX)) {
					extraInputs.loadBalanceMax = new BigDecimal(split[1]);
					log.debug(DCSConstants.LOAD_BALANCE_MAX + ": "
							+ extraInputs.loadBalanceMax);
					index++;
				}
				if (split[0].contains(DCSConstants.PERCENT)) {
					extraInputs.percent = new BigDecimal(split[1]);
					log.debug(DCSConstants.PERCENT + ": "
							+ extraInputs.percent);
					index++;
				}
				if (split[0].contains(DCSConstants.SOFT_CONSTRAINTS)) {
					extraInputs.softConstraints = split[1]
							.contains(DCSConstants.TRUE);
					log.debug(DCSConstants.SOFT_CONSTRAINTS + ": "
							+ extraInputs.softConstraints);
					index++;
				}
				if (split[0].contains(DCSConstants.SOFT_CONSTRAINTS_TYPE)) {
					extraInputs.softConstraintsType = split[1];
					log.debug(DCSConstants.SOFT_CONSTRAINTS_TYPE + ": "
							+ extraInputs.softConstraintsType);
					index++;
				}
				if (split[0].contains(DCSConstants.PRINT_OVERHEAD)) {
					extraInputs.printOverhead = split[1]
							.contains(DCSConstants.TRUE);
					log.debug(DCSConstants.PRINT_OVERHEAD + ": "
							+ extraInputs.printOverhead);
					index++;
				}
				if (split[0].contains(DCSConstants.SELECTION_MODE)) {
					extraInputs.selectionMode = split[1];
					log.debug(DCSConstants.SELECTION_MODE + ": "
							+ extraInputs.printOverhead);
					index++;
				}
			}
		}
		log.debug(index + " constraints loaded!");
		//
		log.debug("Loading aditional inputs");
		index = 0;
		boolean readingFunction = false;
		for (String line : file) {
			if (line.trim().contains(DCSConstants.FUNCTION_BEGIN)) {
				readingFunction = true;
				continue;
			}
			if (line.trim().contains(DCSConstants.FUNCTION_END)) {
				break;
			}
			if (!readingFunction) {
				continue;
			}
			String[] splitAux = line.split("\\s+");
			if (splitAux.length > 1) {
				String[] split = splitAux[1].split("=");
				if (split[0].contains(DCSConstants.POPULATION)) {
					extraInputs.population = new BigDecimal(split[1]);
					log.debug(DCSConstants.POPULATION + ": "
							+ extraInputs.population);
					index++;
				}
				if (split[0].contains(DCSConstants.GENERATIONS)) {
					extraInputs.generations = new BigDecimal(split[1]);
					log.debug(DCSConstants.GENERATIONS + ": "
							+ extraInputs.generations);
					index++;
				}
				if (split[0].contains(DCSConstants.OBJECTIVES_FUNCTIONS)) {
					extraInputs.objectivesFunctions = new BigDecimal(split[1]);
					log.debug(DCSConstants.OBJECTIVES_FUNCTIONS + ": "
							+ extraInputs.objectivesFunctions);
					index++;
				}
			}
		}
		log.debug("Aditional inputs loaded!");
		return extraInputs;
	}

	private List<String> readFile(String filePath) {
		BufferedReader br = null;
		String lineStr = "";
		List<String> fileReaded = new ArrayList<String>();
		try {
			log.info("Reading [" + filePath + "]");
			br = new BufferedReader(new FileReader(filePath));
			int rowCount = 0;
			while ((lineStr = br.readLine()) != null) {
				rowCount++;
				fileReaded.add(lineStr);
			}
			log.info(rowCount + " lines readed");
		} catch (Exception e) {
			log.error("Reading file", e);
			return null;
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					log.error("Closing file", e);
					return null;
				}
			}
		}
		return fileReaded;
	}

	// **********************************************************
	public String[] getProviders() {
		String[] providers = new String[cloudProviders.size()];
		for (CloudProvider cp : cloudProviders) {
			providers[cp.k] = cp.name;
		}
		return providers;
	}

	public String[] getInstanceName() {
		String[] instanceName = new String[instanceTypes.size()];
		for (InstanceType instanceType : instanceTypes) {
			instanceName[instanceType.j] = instanceType.label;
		}
		return instanceName;
	}

	public BigDecimal[] getInstanceCores() {
		BigDecimal[] instanceCores = new BigDecimal[instanceTypes.size()];
		for (InstanceType instanceType : instanceTypes) {
			instanceCores[instanceType.j] = instanceType.cores;
		}
		return instanceCores;
	}

	public BigDecimal[] getInstanceMemory() {
		BigDecimal[] instanceMemory = new BigDecimal[instanceTypes.size()];
		for (InstanceType instanceType : instanceTypes) {
			instanceMemory[instanceType.j] = instanceType.memory;
		}
		return instanceMemory;
	}

	public BigDecimal[][] getPrices() {
		int l = instanceTypes.size();
		int k = cloudProviders.size();
		BigDecimal[][] prices = new BigDecimal[k][l];
		for (CloudProvider cp : cloudProviders) {
			for (InstanceType instanceType : instanceTypes) {
				prices[cp.k][instanceType.j] = instanceType
						.getPricePerProvider(cp);
			}
		}
		return prices;
	}

	public BigDecimal[][] getMigrationStatisticsOverhead() {
		int l = instanceTypes.size();
		int k = cloudProviders.size();
		BigDecimal[][] migrationSatisticsOverhead = new BigDecimal[k * 2][l];
		for (int i = 0; i < k * 2; i++) {
			for (int j = 0; j < l; j++) {
				migrationSatisticsOverhead[i][j] = new BigDecimal(-1);
			}
		}
		for (MigrationStatistics m : migrationStatistics) {
			migrationSatisticsOverhead[m.cloudProvider.k * 2][m.instanceType.j] = m.allocation;
			migrationSatisticsOverhead[m.cloudProvider.k * 2 + 1][m.instanceType.j] = m.deAllocation;
		}
		return migrationSatisticsOverhead;
	}

	public BigDecimal[] getUserRequirements() {
		BigDecimal[] clientRequirements = new BigDecimal[9];
		clientRequirements[DCSConstants.VM_TO_DEPLOY_INDEX] = extraInputs.vmToDeploy;
		clientRequirements[DCSConstants.CPU_MIN_INDEX] = extraInputs.cpuMin;
		clientRequirements[DCSConstants.CPU_MAX_INDEX] = extraInputs.cpuMax;
		clientRequirements[DCSConstants.MEM_MIN_INDEX] = extraInputs.memMin;
		clientRequirements[DCSConstants.MEM_MAX_INDEX] = extraInputs.memMax;
		clientRequirements[DCSConstants.BUDGET_MIN_INDEX] = extraInputs.budgetMin;
		clientRequirements[DCSConstants.BUDGET_MAX_INDEX] = extraInputs.budgetMax;
		clientRequirements[DCSConstants.LOAD_BALANCE_MIN_INDEX] = extraInputs.loadBalanceMin;
		clientRequirements[DCSConstants.LOAD_BALANCE_MAX_INDEX] = extraInputs.loadBalanceMax;
		return clientRequirements;
	}

	public ExtraInputs getExtraInputs() {
		return extraInputs;
	}

}
