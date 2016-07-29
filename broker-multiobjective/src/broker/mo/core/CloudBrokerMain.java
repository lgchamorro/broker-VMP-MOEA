package broker.mo.core;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import broker.mo.util.DCSConstants;
import broker.mo.util.LoadInputs;

/**
 * 
 * @author Lino Chamorro
 * 
 */
public class CloudBrokerMain {

	final Logger log = Logger.getLogger(getClass());

	public static void main(String[] args) {
		CloudBrokerMain cloudBroker = new CloudBrokerMain();
		if (args == null || args.length < 1) {
			cloudBroker.log.error("Missing arguments");
			return;
		}
		cloudBroker.log.debug("BEGIN");
		int runs = Integer.parseInt(args[0]);
		List<Run> solutionsRunned = new ArrayList<Run>();
		List<Front> solutionFront;
		List<Individual> selectedSolutions;
		Run run;
		for (int r = 0; r < runs; r++) {
			NSGA nsga = new NSGA();
			CloudBrokerInstance instance = new CloudBrokerInstance();
			run = new Run();
			solutionFront = new ArrayList<Front>();
			selectedSolutions = new ArrayList<Individual>();
			run.runNo = r;
			cloudBroker.log.info("Run: " + r);
			for (int i = 1; i < args.length; i++) {
				Front currentFront = new Front();
				LoadInputs loadInputs = new LoadInputs();
				loadInputs.processInput(args[i]);
				// load problem input
				if (!instance.init(loadInputs.getProviders(),
						loadInputs.getInstanceName(),
						loadInputs.getInstanceCores(),
						loadInputs.getInstanceMemory(), loadInputs.getPrices(),
						loadInputs.getMigrationStatisticsOverhead(),
						loadInputs.getUserRequirements(),
						loadInputs.getExtraInputs())) {
					cloudBroker.log.error("Initializing parameters");
					return;
				}
				instance.printInput();
				// run NSGA
				nsga.runNSGA(instance, i);
				// add output for ti
				currentFront.individuals = instance.paretoFront;
				selectedSolutions.add(instance.selectedSolution);
				//
				solutionFront.add(currentFront);
			}
			// add to run
			run.solutionFront = solutionFront;
			run.selectedSolutions = selectedSolutions;
			solutionsRunned.add(run);
		}
		cloudBroker.log.info("END of excecutions\n\n");
		int runCount = 0;
		Front front;
		Individual selectedIndividual;
		for (Run printRun : solutionsRunned) {
			// print front
			for (int tCount = 0; tCount < printRun.solutionFront.size(); tCount++) {
				front = printRun.solutionFront.get(tCount);
				selectedIndividual = printRun.selectedSolutions.get(tCount);
				cloudBroker.log.info("***** RUN: " + runCount + ", t: "
						+ tCount + ", front size: " + front.individuals.size()
						+ " *****");
				if (selectedIndividual != null) {
					// printOutput(runCount, front.individuals);
					printOutputCSV(runCount, tCount, front.individuals,
							selectedIndividual, front.individuals.size());
				}

			}
			runCount++;
		}
	}

	public static void printOutputCSV(int run, int t,
			List<Individual> solutions, Individual selectedIndividual,
			int frontSize) {
		CloudBrokerMain cloudBroker = new CloudBrokerMain();
		CloudBrokerInstance instance = new CloudBrokerInstance();
		//
		StringBuilder sb = new StringBuilder();
		// ----------------------------
		// int solutionNro = 0;
		sb.append("\n\nRun" + DCSConstants.SEPARATOR + "t"
				+ DCSConstants.SEPARATOR + "SolutionNro"
				+ DCSConstants.SEPARATOR + "Population"
				+ DCSConstants.SEPARATOR + "Generation"
				+ DCSConstants.SEPARATOR + "TIC" + DCSConstants.SEPARATOR
				+ "TIC_OVERHEAD" + DCSConstants.SEPARATOR + "TIM"
				+ DCSConstants.SEPARATOR + "TIM_OVERHEAD"
				+ DCSConstants.SEPARATOR + "TIP" + DCSConstants.SEPARATOR
				+ "MinLoadIndex" + DCSConstants.SEPARATOR + "CrowdedDistance"
				+ DCSConstants.SEPARATOR + frontSize + "\n");
		// for (Individual individual : solutions) {
		// sb.append(run + DCSConstants.SEPARATOR + t + DCSConstants.SEPARATOR
		// + solutionNro + DCSConstants.SEPARATOR
		// + instance.getCSVPopulationGenerationTicTip(individual)
		// + DCSConstants.SEPARATOR + frontSize + "\n");
		// solutionNro++;
		// }
		sb.append(run + DCSConstants.SEPARATOR + t + DCSConstants.SEPARATOR
				+ "SELECTED" + DCSConstants.SEPARATOR
				+ instance.getCSVPopulationGenerationTicTip(selectedIndividual)
				+ DCSConstants.SEPARATOR + frontSize + "\n");
		sb.append(instance.placementToString(selectedIndividual) + "\n");
		cloudBroker.log.info(sb.toString());
		// ----------------------------
		// sb = new StringBuilder();
		// sb.append("\n\nRun" + DCSConstants.SEPARATOR + "t"
		// + DCSConstants.SEPARATOR + "LOC\n");
		// for (Individual individual : solutions) {
		// sb.append(run + DCSConstants.SEPARATOR + t + DCSConstants.SEPARATOR
		// + instance.getCSVLoc(individual));
		// }
		// cloudBroker.log.info(sb.toString());
		// ----------------------------
		// sb = new StringBuilder();
		// sb.append("\n\nRun" + DCSConstants.SEPARATOR + "t"
		// + DCSConstants.SEPARATOR + "SolutionNro"
		// + DCSConstants.SEPARATOR + "Generation"
		// + DCSConstants.SEPARATOR + "Population"
		// + DCSConstants.SEPARATOR + "Provider" + DCSConstants.SEPARATOR
		// + "Instance" + DCSConstants.SEPARATOR + "Capacity"
		// + DCSConstants.SEPARATOR + "Price" + DCSConstants.SEPARATOR
		// + "Qty" + DCSConstants.SEPARATOR + "TotalCore"
		// + DCSConstants.SEPARATOR + "TotalMemory"
		// + DCSConstants.SEPARATOR + "MigTime" + DCSConstants.SEPARATOR
		// + "MigOverhead" + DCSConstants.SEPARATOR + "TotalPrice"
		// + DCSConstants.SEPARATOR + "MinLoadIndex"
		// + DCSConstants.SEPARATOR + "CrowdedDistance" + "\n");
		// int solutionNro = 0;
		// for (Individual individual : solutions) {
		// sb.append(instance.getCSVInstancePerProviderSummary(run, t,
		// solutionNro, individual) + "\n");
		// solutionNro++;
		// }
		// cloudBroker.log.info(sb.toString());
	}

	public static void printOutput(int t, List<Individual> solutions) {
		CloudBrokerInstance instance = new CloudBrokerInstance();
		int i = 0;
		for (Individual individual : solutions) {
			instance.printOutput(individual, i);
		}
	}
}
