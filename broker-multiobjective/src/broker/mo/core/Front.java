package broker.mo.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Lino Chamorro
 * 
 */
public class Front {

	int frontIndex;
	List<Individual> individuals;

	Front() {
		super();
		this.frontIndex = 0;
		this.individuals = new ArrayList<Individual>();
	}

	Front(int frontIndex, List<Individual> front) {
		super();
		this.frontIndex = frontIndex;
		this.individuals = front;
	}

}
