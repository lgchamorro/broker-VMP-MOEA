package broker.mo.core;

import java.util.List;

/**
 * 
 * @author Lino Chamorro
 * 
 */
public class Run {

	int runNo;
	List<Front> solutionFront;
	List<Individual> selectedSolutions;

	public Run() {
		super();
	}

	public Run(int run, List<Front> solutionFront) {
		super();
		this.runNo = run;
		this.solutionFront = solutionFront;
	}

}
