package broker.mo.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import broker.mo.util.DCSConstants;
import broker.mo.util.DCSUtil;
import broker.mo.util.QuickSortV2;

/**
 * 
 * @author Lino Chamorro
 * 
 */
public class NSGA {

	private final Logger log = Logger.getLogger(getClass());

	CloudBrokerInstance instance;
	List<Individual> knownFeasibleSolution;
	/*
	 * Best solutions
	 */
	List<Individual> paretoFront;

	void runNSGA(CloudBrokerInstance instance, int run) {
		/*
		 * almacenar solucion anterior para comparar con la solucion actual
		 */
		if (run > 1) {
			paretoFront = null;
			instance.prevSolution = instance.selectedSolution;
		}
		knownFeasibleSolution = new ArrayList<Individual>();
		this.instance = instance;
		int populationCount = instance.extraInputs.population.intValue();
		int generations = instance.extraInputs.generations.intValue();
		List<Front> fronts = null;
		// first generation
		int generationCount = 0;
		log.debug("*** Generation: [" + (generationCount++) + "] ***");
		List<Individual> initialPopulation = this
				.getFirstGeneration(populationCount);
		int t = 0;
		// infeasible solutions reparation
		List<Individual> repairedInitialPopulaiton = this
				.repairPopulationLOCmin(initialPopulation, true);
		List<Individual> pt = repairedInitialPopulaiton;
		fronts = this.NonDominatedSort(pt);
		this.updateParetoFront(fronts.get(0).individuals);
		List<Individual> qt0 = null;
		List<Individual> qt1 = null;
		List<Individual> qt1Repaired = null;
		List<Individual> qt2 = null;
		while (t < generations) {
			log.debug("*** Generation: [" + (generationCount++) + "] ***");
			qt0 = singlePointCossOver(pt);
			qt1 = bitWiseMutation(qt0);
			qt1Repaired = this.repairPopulationLOCmin(qt1, false);
			qt2 = this.union(qt1Repaired, pt);// Pt U Qc
			log.debug("Pt U Qc size: " + qt2.size());
			fronts = this.NonDominatedSort(qt2);
			this.updateParetoFront(fronts.get(0).individuals);
			t++;
			if (paretoFront != null && paretoFront.size() > 0) {
				log.trace("guarda t: " + t);
			}
			if (t < generations) {
				pt = getBestIndividuals(fronts, populationCount);
			}
		}
		instance.solutionFound = true;
		instance.paretoFront = this.paretoFront;
		instance.selectedSolution = this.getBestIndividual(this.paretoFront);
	}

	List<Individual> getBestIndividuals(List<Front> fronts, int populationCount) {
		log.debug("getting best individuals..");
		List<Individual> selectedPopulation = new ArrayList<Individual>();
		List<Individual> qfront;
		int index = 0;
		boolean end = false;
		for (Front f : fronts) {
			this.setCrowdingDistance(f.individuals);
			qfront = sortByCrowdingDistance(f.individuals);
			log.trace("Sorted front");
			this.printSortedFront(qfront);
			log.trace("---");
			for (Individual i : qfront) {
				selectedPopulation.add(i);
				index++;
				if (index == populationCount) {
					log.trace("Individuals selected = Population. End selection");
					end = true;
					break;
				}
			}
			if (end) {
				break;
			}
		}
		log.trace("Best individuals selected: " + selectedPopulation.size());
		return selectedPopulation;
	}

	void printSortedFront(List<Individual> qFront) {
		for (Individual i : qFront) {
			log.trace("\t\t" + i);
		}
	}

	Individual getBestIndividual(List<Individual> qFront) {
		String selectionMode = instance.extraInputs.selectionMode;
		Individual selected = null;
		if (selectionMode.equals(DCSConstants.ALEATORY)) {
			int size = qFront.size();
			selected = qFront.get(DCSUtil.getRandom(size));
		} else if (selectionMode.equals(DCSConstants.DISTANCE)) {
			this.setCrowdingDistance(qFront);
			for (Individual i : qFront) {
				if (selected == null) {
					selected = i;
				} else {
					if (selected.compareByOriginDistance(i) < 0) {
						selected = i;
					}
				}
			}
		} else if (selectionMode.equals(DCSConstants.PREFERRED_CPU)) {
			for (Individual i : qFront) {
				if (selected == null) {
					selected = i;
				} else {
					if (selected.ticWithOverhead.compareTo(i.tic) < 0) {
						selected = i;
					}
				}
			}
		} else if (selectionMode.equals(DCSConstants.PREFERRED_MEM)) {
			for (Individual i : qFront) {
				if (selected == null) {
					selected = i;
				} else {
					if (selected.timWithOverhead.compareTo(i.tim) < 0) {
						selected = i;
					}
				}
			}
		} else if (selectionMode.equals(DCSConstants.PREFERRED_PRICE)) {
			for (Individual i : qFront) {
				if (selected == null) {
					selected = i;
				} else {
					if (selected.tip.compareTo(i.tip) > 0) {
						selected = i;
					}
				}
			}
		} else if (selectionMode.equals(DCSConstants.PREFERRED_LOC)) {
			for (Individual i : qFront) {
				if (selected == null) {
					selected = i;
				} else {
					if (selected.getMinLoadIndex() < i.getMinLoadIndex()) {
						selected = i;
					}
				}
			}
		} else if (selectionMode.equals(DCSConstants.PREFERRED_OBJECTIVES)) {
			List<Individual> candidatesList = new ArrayList<Individual>();
			this.setCrowdingDistance(qFront);
			for (Individual currentIndividual : qFront) {
				boolean isPrefered = true;
				for (Individual compIndividual : qFront) {
					// currentIndividual individual worst compIndividual
					if (currentIndividual.compareByObjective(compIndividual) == -1) {
						isPrefered = false;
						break;
					}
				}
				if (isPrefered) {
					candidatesList.add(currentIndividual);
				}
			}
			if (candidatesList.size() == 0) {
				candidatesList = qFront;
			}
			int randomIndex = DCSUtil.getRandom(candidatesList.size());
			selected = candidatesList.get(randomIndex);
		}
		log.debug("Solution selected. " + selected);
		return selected;
	}

	List<Front> NonDominatedSort(List<Individual> population) {
		log.debug("Begin non dominated sort. Size: " + population.size());
		List<Front> sortedFronts = new ArrayList<Front>();
		List<Individual> sP;
		int nP;
		// front counter
		int frontIndex = 1;
		Front currentFront = new Front();
		currentFront.frontIndex = frontIndex;
		int currentIndividual = 0;
		for (Individual p : population) {
			sP = new ArrayList<Individual>();
			nP = 0;
			int comparingIndividual = 0;
			int comparing;
			for (Individual q : population) {
				if (currentIndividual != comparingIndividual) {
					comparing = p.compareTo(q);
					// p dominate q
					if (comparing > 0) {
						log.trace("\n\t->" + p + "\n\t->dominates\n\t->" + q);
						sP.add(q);
					}
					// p is dominated by q
					if (comparing < 0) {
						log.trace("\n\t->" + p + "\n\t->is dominated by\n\t->"
								+ q);
						nP++;
					}
				}
				comparingIndividual++;
			}
			p.sP = sP;
			p.nP = nP;
			if (nP == 0) {
				p.rank = frontIndex;
				currentFront.individuals.add(p);
			}
			currentIndividual++;
		}
		sortedFronts.add(currentFront);
		log.debug("Front: " + currentFront.frontIndex + " added! Front size: "
				+ currentFront.individuals.size());
		//
		Front qFront;
		while (!currentFront.individuals.isEmpty()) {
			qFront = new Front();
			++frontIndex;
			for (Individual p : currentFront.individuals) {
				for (Individual q : p.sP) {
					q.nP--;
					if (q.nP == 0) {
						q.rank = frontIndex;
						qFront.individuals.add(q);
					}
				}
			}
			if (!qFront.individuals.isEmpty()) {
				qFront.frontIndex = frontIndex;
				currentFront = qFront;
				log.debug("Front: " + currentFront.frontIndex
						+ " added! Front size: "
						+ currentFront.individuals.size());
				sortedFronts.add(currentFront);
			} else {
				break;
			}
		}
		log.debug("End non dominated sort. sortedFronts size: "
				+ sortedFronts.size());
		return sortedFronts;
	}

	List<Individual> sortByCrowdingDistance(List<Individual> qFront) {
		QuickSortV2 qsort = new QuickSortV2();
		List<Individual> list1 = qsort.sort(qFront, false, false, false, false,
				true);
		qsort = new QuickSortV2();
		List<Individual> list2 = qsort.sort(list1, false, false, false, false,
				true);
		qsort = new QuickSortV2();
		List<Individual> list3 = qsort.sort(list2, false, false, false, false,
				true);
		qsort = new QuickSortV2();
		List<Individual> list4 = qsort.sort(list3, false, false, false, false,
				true);
		return list4;
	}

	void setCrowdingDistance(List<Individual> population) {
		log.trace("Setting crowding distance.. population size: "
				+ population.size());
		if (population == null || population.size() == 0) {
			log.error("setCrowdingDistance. population.size == 0 or null");
			return;
		}
		int m = instance.extraInputs.objectivesFunctions.intValue();
		for (Individual individual : population) {
			individual.crowdedDistance = new BigDecimal(0);
		}
		//
		Individual current;
		Individual min;
		Individual individualPrev;
		Individual individualNext;
		Individual max;
		//
		BigDecimal fMin;
		BigDecimal fMax;
		//
		BigDecimal numerador = null;
		BigDecimal denominador = null;
		BigDecimal one = new BigDecimal(1);
		//
		for (int i = 1; i < population.size() - 1; i++) {
			current = population.get(i);
			current.crowdedDistance = new BigDecimal(0);
		}
		//
		for (int objective = 0; objective < m; objective++) {
			boolean sortByTIC = objective == 0;
			boolean sortByTIM = objective == 1;
			boolean sortByTIP = objective == 2;
			boolean sortByLOC = objective == 3;
			population = this.sortByObjectiveFunction(population, sortByTIC,
					sortByTIM, sortByTIP, sortByLOC, false);
			min = population.get(0);
			min.crowdedDistance = new BigDecimal(DCSConstants.INFINITY);
			max = population.get(population.size() - 1);
			max.crowdedDistance = new BigDecimal(DCSConstants.INFINITY);
			if (sortByTIC) {
				fMin = min.ticWithOverhead;
				fMax = max.ticWithOverhead;
			} else if (sortByTIM) {
				fMin = min.timWithOverhead;
				fMax = max.timWithOverhead;
			} else if (sortByTIP) {
				fMin = one.divide(min.tip, 3, RoundingMode.HALF_EVEN);
				fMax = one.divide(max.tip, 3, RoundingMode.HALF_EVEN);
			} else if (sortByLOC) {
				fMin = new BigDecimal(min.getMinLoadIndex());
				fMax = new BigDecimal(max.getMinLoadIndex());
			} else {
				log.error("setCrowdingDistanceAssignament guarda con la funcion objetivo");
				return;
			}
			denominador = fMax.subtract(fMin);
			for (int i = 1; i < population.size() - 1; i++) {
				current = population.get(i);
				individualPrev = population.get(i - 1);
				individualNext = population.get(i + 1);
				if (sortByTIC) {
					numerador = individualNext.ticWithOverhead
							.subtract(individualPrev.ticWithOverhead);
				} else if (sortByTIM) {
					numerador = individualNext.timWithOverhead
							.subtract(individualPrev.timWithOverhead);
				} else if (sortByTIP) {
					BigDecimal tmpPrev = one.divide(individualPrev.tip, 3,
							RoundingMode.HALF_EVEN);
					BigDecimal tmpNext = one.divide(individualNext.tip, 3,
							RoundingMode.HALF_EVEN);
					numerador = tmpNext.subtract(tmpPrev);
				} else if (sortByLOC) {
					BigDecimal tmpPrev = new BigDecimal(
							individualPrev.getMinLoadIndex());
					BigDecimal tmpNext = new BigDecimal(
							individualNext.getMinLoadIndex());
					numerador = tmpNext.subtract(tmpPrev);
				}
				BigDecimal indexTmp = new BigDecimal(0);
				if (denominador.compareTo(new BigDecimal(0)) != 0) {
					indexTmp = numerador.divide(denominador, 3,
							RoundingMode.HALF_EVEN);
				}
				if (indexTmp.doubleValue() < 0) {
					for (Individual iTmp : population) {
						log.debug("\tSorted front by objective: " + objective
								+ ", " + iTmp);
					}
					log.warn("crowded dinstance is negative");
				}
				log.trace("\tCrowding distance objective: " + objective
						+ ", indexTmp: " + indexTmp + ", " + current);
				current.crowdedDistance = current.crowdedDistance.add(indexTmp);
			}
		}
		for (Individual i : population) {
			log.trace("\tCrowded distance setted: " + i);
		}
	}

	List<Individual> sortByObjectiveFunction(List<Individual> qFront,
			boolean sortByTIC, boolean sortByTIM, boolean sortByTIP,
			boolean sortByLOC, boolean sortByCrowdedDistance) {
		QuickSortV2 qsort = new QuickSortV2();
		List<Individual> list1 = qsort.sort(qFront, sortByTIC, sortByTIM,
				sortByTIP, sortByLOC, sortByCrowdedDistance);
		qsort = new QuickSortV2();
		List<Individual> list2 = qsort.sort(list1, sortByTIC, sortByTIM,
				sortByTIP, sortByLOC, sortByCrowdedDistance);
		qsort = new QuickSortV2();
		List<Individual> list3 = qsort.sort(list2, sortByTIC, sortByTIM,
				sortByTIP, sortByLOC, sortByCrowdedDistance);
		qsort = new QuickSortV2();
		List<Individual> list4 = qsort.sort(list3, sortByTIC, sortByTIM,
				sortByTIP, sortByLOC, sortByCrowdedDistance);
		qsort = new QuickSortV2();
		List<Individual> list5 = qsort.sort(list4, sortByTIC, sortByTIM,
				sortByTIP, sortByLOC, sortByCrowdedDistance);
		qsort = new QuickSortV2();
		List<Individual> list6 = qsort.sort(list5, sortByTIC, sortByTIM,
				sortByTIP, sortByLOC, sortByCrowdedDistance);
		qsort = new QuickSortV2();
		List<Individual> list7 = qsort.sort(list6, sortByTIC, sortByTIM,
				sortByTIP, sortByLOC, sortByCrowdedDistance);
		qsort = new QuickSortV2();
		List<Individual> list8 = qsort.sort(list7, sortByTIC, sortByTIM,
				sortByTIP, sortByLOC, sortByCrowdedDistance);
		qsort = new QuickSortV2();
		List<Individual> list9 = qsort.sort(list8, sortByTIC, sortByTIM,
				sortByTIP, sortByLOC, sortByCrowdedDistance);
		qsort = new QuickSortV2();
		List<Individual> list10 = qsort.sort(list9, sortByTIC, sortByTIM,
				sortByTIP, sortByLOC, sortByCrowdedDistance);
		return list10;
	}

	List<Individual> union(List<Individual> list1, List<Individual> list2) {
		List<Individual> union = new ArrayList<Individual>();
		for (Individual i : list1) {
			union.add(i);
		}
		for (Individual i : list2) {
			union.add(i);
		}
		return union;
	}

	void updateParetoFront(List<Individual> list) {
		log.trace("Updating pareto...");
		if (paretoFront == null) {
			paretoFront = new ArrayList<Individual>();
			for (Individual i : list) {
				/*
				 * soft constraints are previously validated
				 */
				if (isSoftConstraintsOK(i)) {
					log.trace("Adding to pareto --> " + i);
					paretoFront.add(i);
				}
			}
		} else {
			list = this.removeEquals(list);
			List<Individual> paretoFrontTmp = new ArrayList<Individual>();
			// first compare new elements
			for (Individual i : list) {
				boolean isDominated = false;
				for (Individual iPareto : paretoFront) {
					if (iPareto.compareTo(i) > 0) {
						log.trace("\n\t->" + i + "\n\t->is dominated by\n\t->"
								+ iPareto);
						isDominated = true;
						break;
					}
				}
				/*
				 * soft constraints are previously validated
				 */
				if (!isDominated && isSoftConstraintsOK(i)) {
					log.trace("Adding to pareto --> " + i);
					paretoFrontTmp.add(i);
				}
			}
			List<Individual> paretoFrontTmp2 = new ArrayList<Individual>();
			// now compare prev pareto front elements
			for (Individual prevParetoElement : paretoFront) {
				boolean isDominated = false;
				for (Individual i : paretoFrontTmp) {
					if (i.compareTo(prevParetoElement) > 0) {
						log.trace("\n\t->" + prevParetoElement
								+ "\n\t->is dominated by\n\t->" + i);
						isDominated = true;
						break;
					}
				}
				/*
				 * soft constraints are previously validated
				 */
				if (!isDominated && isSoftConstraintsOK(prevParetoElement)) {
					log.trace("It keeps in pareto front --> "
							+ prevParetoElement);
					paretoFrontTmp2.add(prevParetoElement);
				}
			}
			paretoFront = this.union(paretoFrontTmp, paretoFrontTmp2);
		}
		log.info("Pareto front updated. Size: " + paretoFront.size());
	}

	boolean isSoftConstraintsOK(Individual individual) {
		if (!instance.extraInputs.softConstraints) {
			return true;
		}
		BigDecimal tic = individual.tic;
		BigDecimal tim = individual.tim;
		BigDecimal tip = individual.tip;
		int minLoc = individual.getMinLoadIndex();
		// Fixed range
		if (instance.extraInputs.softConstraintsType
				.equalsIgnoreCase(DCSConstants.SOFT_RANGE)) {
			BigDecimal cpuMin = instance.extraInputs.cpuMin;
			BigDecimal cpuMax = instance.extraInputs.cpuMax;
			// tic es menos al min o es mayor al max
			if (tic.compareTo(cpuMin) == -1 || tic.compareTo(cpuMax) == 1) {
				log.trace("CPU Soft constraint violated. cpuMin: " + cpuMin
						+ ", cpuMax: " + cpuMax + ", " + individual);
				return false;
			}
			BigDecimal timMin = instance.extraInputs.memMin;
			BigDecimal timMax = instance.extraInputs.memMax;
			// tim es menos al min o es mayor al max
			if (tim.compareTo(timMin) == -1 || tim.compareTo(timMax) == 1) {
				log.trace("MEM Soft constraint violated. timMin: " + timMin
						+ ", timMax: " + timMax + ", " + individual);
				return false;
			}
			BigDecimal tipMin = instance.extraInputs.budgetMin;
			BigDecimal tipMax = instance.extraInputs.budgetMax;
			// tip es menor al min o es mayor al max
			if (tip.compareTo(tipMin) == -1 || tip.compareTo(tipMax) == 1) {
				log.trace("TIP Soft constraint violated. tipMin: " + tipMin
						+ ", tipMax: " + tipMax + ", " + individual);
				return false;
			}
			int lbMin = instance.extraInputs.loadBalanceMin.intValue();
			int lbMax = instance.extraInputs.loadBalanceMax.intValue();
			// minLoc es menor al min o es mayor al max
			if (minLoc < lbMin || minLoc > lbMax) {
				log.trace("TIP Soft constraint violated. lbMin: " + lbMin
						+ ", lbMax: " + lbMax + ", " + individual);
				return false;
			}
			// Fixed percent
		} else if (instance.extraInputs.softConstraintsType
				.equalsIgnoreCase(DCSConstants.SOFT_PERCENT)) {
			BigDecimal percent = instance.extraInputs.percent;
			// FIXME tic validar
			BigDecimal cpuExpected = instance.extraInputs.cpuExpected;
			BigDecimal cpuMin = DCSUtil.getMinPercent(cpuExpected, percent);
			BigDecimal cpuMax = DCSUtil.getMaxPercent(cpuExpected, percent);
			// tic es menos al min o es mayor al max
			if (cpuExpected.intValue() > 0
					&& (tic.compareTo(cpuMin) == -1 || tic.compareTo(cpuMax) == 1)) {
				log.trace("CPU Soft percent constraint violated. cpuExpected: "
						+ cpuExpected + ", cpuMin: " + cpuMin + ", cpuMax: "
						+ cpuMax + ", " + individual);
				return false;
			}
			BigDecimal memExpected = instance.extraInputs.memExpected;
			BigDecimal timMin = DCSUtil.getMinPercent(memExpected, percent);
			BigDecimal timMax = DCSUtil.getMaxPercent(memExpected, percent);
			// tim es menos al min o es mayor al max
			if (memExpected.intValue() > 0
					&& (tim.compareTo(timMin) == -1 || tim.compareTo(timMax) == 1)) {
				log.trace("MEM Soft percent constraint violated. memExpected: "
						+ memExpected + ", timMin: " + timMin + ", timMax: "
						+ timMax + ", " + individual);
				return false;
			}
			// FIXME tip validar
			BigDecimal budgetExpected = instance.extraInputs.budgetExpected;
			BigDecimal tipMin = DCSUtil.getMinPercent(budgetExpected, percent);
			BigDecimal tipMax = DCSUtil.getMaxPercent(budgetExpected, percent);
			// tip es menor al min o es mayor al max
			if (budgetExpected.intValue() > 0
					&& (tip.compareTo(tipMin) == -1 || tip.compareTo(tipMax) == 1)) {
				log.trace("TIP Soft percent constraint violated. budgetExpected: "
						+ budgetExpected
						+ " tipMin: "
						+ tipMin
						+ ", tipMax: "
						+ tipMax + ", " + individual);
				return false;
			}
		} else {
			log.error("Invalid softConstraint Type defined: "
					+ instance.extraInputs.softConstraintsType);
		}
		return true;
	}

	List<Individual> removeEquals(List<Individual> list) {
		List<Individual> listWOequals = new ArrayList<Individual>();
		int index = 0;
		for (Individual i : list) {
			boolean haveEquals = false;
			int comparatorIndex = 0;
			for (Individual compare : list) {
				if (index != comparatorIndex && i.equals(compare)) {
					haveEquals = true;
					break;
				}
				comparatorIndex++;
			}
			if (!haveEquals) {
				listWOequals.add(i);
			}
			index++;
		}
		return listWOequals;
	}

	List<Individual> getFirstGeneration(int populationCount) {
		List<Individual> population = new ArrayList<Individual>();
		Individual individual;
		for (int i = 0; i < populationCount; i++) {
			individual = generateRandomIndividual();
			population.add(individual);
			log.trace(i + " - " + individual.toString());
		}
		return population;
	}

	Individual generateRandomIndividual() {
		int n = instance.extraInputs.vmToDeploy.intValue();
		int l = instance.instanceName.length;
		int m = instance.providers.length;
		Individual individual = new Individual(this.generateRandomSolution(n,
				l, m, instance.prices));
		setIndividualsParameters(individual);
		return individual;
	}

	BigDecimal[][] generateRandomSolution(int n, int l, int m,
			BigDecimal[][] prices) {
		log.trace("Generating random individual");
		BigDecimal[][] output = new BigDecimal[2][n];
		int instanceType;
		int cloudProvider;
		double price;
		boolean isValid;
		for (int i = 0; i < n; i++) {
			isValid = false;
			do {
				instanceType = DCSUtil.getRandom(l);
				cloudProvider = DCSUtil.getRandom(m);
				price = prices[cloudProvider][instanceType].doubleValue();
				if (price > 0) {
					isValid = true;
				} else {
					log.trace("Discarding unfeasible random");
				}
			} while (!isValid);
			output[0][i] = new BigDecimal(instanceType);
			output[1][i] = new BigDecimal(cloudProvider);
		}
		return output;
	}

	List<Individual> repairPopulationLOCmin(List<Individual> population,
			boolean isFirstGeneration) {
		List<Individual> populationWithSoftConstraintOk = new ArrayList<Individual>();
		int n = population.size();
		int count = 0;
		/*
		 * regenerate individuals that not meet soft constraints
		 */
		if (isFirstGeneration) {
			for (Individual individual : population) {
				// if (isSoftConstraintsOK(individual)) {
				// populationWithSoftConstraintOk.add(individual);
				// } else if (count < (n * 0.05)) { // FIXME Validar esto
				// generar de vuelta si la cantidad de soluciones
				// generadas es menor al 5% de la poblacion
				// while (true) {
				// Individual random = generateRandomIndividual();
				// if (isSoftConstraintsOK(random)) {
				// populationWithSoftConstraintOk.add(random);
				// break;
				// }
				// }
				// } else {
				// si no, agregar tal cual, total antes de actualizar el
				// pareto front se vuelve a validar
				populationWithSoftConstraintOk.add(individual);
				// }
				count++;
			}
		} else {
			populationWithSoftConstraintOk = population;
		}
		/*
		 * repair solutions that do not meet load balance constraint
		 */
		List<Individual> repairedPopulation = new ArrayList<Individual>();
		for (Individual individual : populationWithSoftConstraintOk) {
			boolean isOkLoc = false;
			while (true) {
				if (!isFeasibleSolutionLoc(individual)) {
					individual = repairLOCmin(individual);
				} else {
					isOkLoc = true;
				}
				if (isOkLoc) {
					break;
				}
			}
			knownFeasibleSolution.add(individual);
			repairedPopulation.add(individual);
		}
		return repairedPopulation;
	}

	/**
	 * Repair LOC constraint
	 * 
	 * @param i
	 * @return
	 */
	Individual repairLOCmin(Individual i) {
		log.trace("Repairing LOCmin solution. " + i);
		int n = instance.extraInputs.vmToDeploy.intValue();
		int kMax;
		int kMin;
		int vmi = -1;
		while (n > 0) {
			kMax = DCSUtil.getMax(i.loc);
			kMin = DCSUtil.getMin(i.loc);
			// get random VM
			vmi = this.selectRandomVmFromKmax(i, kMax, kMin);
			// migrar vm de kMax a kMin
			i.output[1][vmi] = new BigDecimal(kMin);
			log.trace("Migrating.. vm: " + vmi + " from provider: " + kMax
					+ " to provider: " + kMin);
			setIndividualsParameters(i);
			if (isFeasibleSolutionLoc(i)) {
				log.trace("Solution repaired!. LOCmin " + i);
				return i;
			} else {
				n--;
			}
		}
		/*
		 * Ninguna migracion fue realizada
		 */
		if (n == 0) {
			log.debug("LOCmin can't be repaired" + i);
			log.debug("Getting known feasible solution for LOC");
			if (knownFeasibleSolution.size() == 0) {
				log.error("Solution can't be repaired for LOC constraints");
				return null;
			}
			int random = DCSUtil.getRandom(knownFeasibleSolution.size());
			i = knownFeasibleSolution.get(random);
		}
		return i;
	}

	boolean isFeasibleSolutionLoc(Individual individual) {
		BigDecimal locMin = instance.extraInputs.loadBalanceMin;
		int n = instance.extraInputs.vmToDeploy.intValue();
		double load;
		double loadIndex;
		for (int i = 0; i < individual.loc.length; i++) {
			load = individual.loc[i].doubleValue();
			loadIndex = load / (double) n;
			if (loadIndex < locMin.doubleValue()) {
				log.trace("Unfeasible solution. LOCmin constraints. LOC: "
						+ loadIndex + ", LOCmin: " + locMin.doubleValue()
						+ ", load: " + individual.loc[i] + ", provider:" + i);
				return false;
			}
		}
		return true;
	}

	int selectRandomVmFromKmax(Individual i, int kMax, int kMin) {
		int vmSelected = -1;
		int count = 0;
		/*
		 * cargar en una estructura auxiliar las instancias que se despliegan en
		 * kMax
		 */
		int vms = instance.extraInputs.vmToDeploy.intValue();
		for (int j = 0; j < vms; j++) {
			if (i.output[1][j].intValue() == kMax) {
				count++;
			}
		}
		/*
		 * x = VMi. j = Provider k
		 */
		BigDecimal[][] aux = new BigDecimal[2][count];
		/*
		 * count es el indice de la estructura auxiliar
		 */
		count = 0;
		for (int j = 0; j < vms; j++) {
			if (i.output[1][j].intValue() == kMax) {
				aux[0][count] = new BigDecimal(j);
				aux[1][count++] = i.output[1][j];
			}
		}
		/*
		 * estructura auxiliar cargada
		 */
		int n = count;
		boolean[] visited = new boolean[n];
		for (int j = 0; j < visited.length; j++) {
			visited[j] = false;
		}
		int g;
		int vmi;
		/*
		 * instance Type
		 */
		BigDecimal j;
		/*
		 * precio de ejecutar en el proveedor menos cargado
		 */
		BigDecimal priceMinLoad;
		while (n > 0) {
			do {
				// get random from kmax
				g = DCSUtil.getRandom(visited.length);
				vmi = aux[0][g].intValue();
			} while (visited[g]);
			j = i.output[0][vmi];
			priceMinLoad = instance.prices[kMin][j.intValue()];
			/*
			 * La instancia seleccionada no se puede ejecutar en el proveedor
			 * seleccionado
			 */
			if (priceMinLoad.doubleValue() < 0) {
				n--;
				visited[g] = true;
				continue;
			}
			vmSelected = vmi;
			break;
		}
		aux = null;
		return vmSelected;
	}

	// Single point crossover
	List<Individual> singlePointCossOver(List<Individual> population) {
		List<Individual> selectedIndividuals = new ArrayList<Individual>();
		Individual parent1;
		Individual parent2;
		Individual child1;
		Individual child2;
		BigDecimal[][] output1;
		BigDecimal[][] output2;
		int crossPoint;
		int n = instance.extraInputs.vmToDeploy.intValue();
		int i = 0;
		// this.setCrowdingDistance(population);
		if (population.size() % 2 != 0) {
			selectedIndividuals.add(tournamentSelection(population,
					DCSConstants.ENE_TOURNAMENT));
		}
		for (; i < (population.size() / 2); i++) {
			parent1 = tournamentSelection(population,
					DCSConstants.ENE_TOURNAMENT);
			parent2 = tournamentSelection(population,
					DCSConstants.ENE_TOURNAMENT);
			/*
			 * probabilidad de cruzamiento del 50%
			 */
			int random = DCSUtil.getRandom(100);
			if (random % 2 == 0) {
				crossPoint = DCSUtil.getRandom(n);
			} else {
				crossPoint = n;
			}
			output1 = new BigDecimal[2][n];
			output2 = new BigDecimal[2][n];
			for (int j = 0; j < n; j++) {
				output1[0][j] = parent1.output[0][j];
				output1[1][j] = parent1.output[1][j];
				output2[0][j] = parent2.output[0][j];
				output2[1][j] = parent2.output[1][j];
			}
			if (crossPoint != n) {
				log.trace("Crossing..");
				for (int j = crossPoint; j < n; j++) {
					output1[0][j] = parent2.output[0][j];
					output1[1][j] = parent2.output[1][j];
					output2[0][j] = parent1.output[0][j];
					output2[1][j] = parent1.output[1][j];
				}
			}
			child1 = new Individual(output1);
			setIndividualsParameters(child1);
			child2 = new Individual(output2);
			setIndividualsParameters(child2);
			selectedIndividuals.add(child1);
			selectedIndividuals.add(child2);

		}
		population = null;
		return selectedIndividuals;
	}

	Individual tournamentSelection(List<Individual> population, int n) {
		this.setCrowdingDistance(population);
		List<Individual> tournamentIndividual = new ArrayList<Individual>();
		int[] selected = new int[n];
		for (int i = 0; i < selected.length; i++) {
			selected[i] = -1;
		}
		for (int i = 0; i < n; i++) {
			int index = DCSUtil.getRandom(n);
			while (alreadySelected(index, selected)) {
				index = DCSUtil.getRandom(n);
			}
			tournamentIndividual.add(population.get(index));
		}
		// Do tournament
		Individual selectedIndividual = tournamentIndividual.get(0);
		for (int i = 1; i < n; i++) {
			Individual contender = tournamentIndividual.get(i);
			if (selectedIndividual.crowdedComparison(contender) < 0) {
				// if (selectedIndividual.compareTo(contender) < 0) {
				selectedIndividual = contender;
			}
		}
		return selectedIndividual;
	}

	boolean alreadySelected(int index, int[] selected) {
		for (int i = 0; i < selected.length; i++) {
			if (selected[i] == index) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param population
	 * @return
	 */
	List<Individual> bitWiseMutation(List<Individual> population) {
		List<Individual> individualMutated = new ArrayList<Individual>();
		int instanceType;
		int cloudProvider;
		double price;
		boolean isValid;
		int l = instance.instanceName.length;
		int m = instance.providers.length;
		int n = instance.extraInputs.vmToDeploy.intValue();
		int vm;
		for (Individual individual : population) {
			if (mutationProbability()) {
				vm = DCSUtil.getRandom(n);
				log.trace("Mutating. vm: " + vm + ", " + individual);
				isValid = false;
				do {
					instanceType = DCSUtil.getRandom(l);
					cloudProvider = DCSUtil.getRandom(m);
					price = instance.prices[cloudProvider][instanceType]
							.doubleValue();
					if (price > 0) {
						isValid = true;
					}
				} while (!isValid);
				individual.output[0][vm] = new BigDecimal(instanceType);
				individual.output[1][vm] = new BigDecimal(cloudProvider);
				setIndividualsParameters(individual);
				log.trace("Mutated! " + individual);
			} else {
				log.trace("No mutation probability");
			}
			individualMutated.add(individual);
		}
		return individualMutated;
	}

	boolean mutationProbability() {
		// change if decision variables change
		int random = DCSUtil.getRandom(100);
		if (random % 2 == 0) {
			return true;
		}
		return false;
	}

	void setIndividualsParameters(Individual individual) {
		individual.n = instance.extraInputs.vmToDeploy.intValue();
		individual.tic = instance.getTIC(individual.output);
		individual.tim = instance.getTIM(individual.output);
		individual.tip = instance.getTIP(individual.output);
		individual.loc = instance.getLOC(individual.output);
		//
		individual.downtimeStatistics = instance.downtimeStatistics;
		individual.extraInputs = instance.extraInputs;
		individual.providers = instance.providers;
		individual.instanceName = instance.instanceName;
		individual.instanceCores = instance.instanceCores;
		individual.prices = instance.prices;
		individual.instanceSummary = instance.getInstanceSummary(individual);
		if (instance.prevSolution != null) {
			individual.migrationTime = instance.getMigrationTime(individual);
			individual.migrationOverheadCore = instance.getMigrationOverhead(
					individual, true, false);
			individual.migrationOverheadMemory = instance.getMigrationOverhead(
					individual, false, true);
			individual.mcCore = instance.getMCCore(individual);
			individual.mcMemory = instance.getMCMemory(individual);
			individual.prevSolution = instance.prevSolution;
			individual.extraInputs = instance.extraInputs;
		}
		individual.instancePerProviderSummary = instance
				.getInstancePerProviderSummary(individual);
		individual.ticWithOverhead = individual.tic.subtract(individual.mcCore);
		// if (individual.ticWithOverhead == null) {
		// log.debug("setIndividualsParameters. ticWithOverhead is null");
		// }
		individual.timWithOverhead = individual.tim
				.subtract(individual.mcMemory);
	}

}
