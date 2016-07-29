package broker.mo.util;

import java.util.ArrayList;
import java.util.List;

import broker.mo.core.Individual;

/**
 * 
 * @author Lino Gabriel
 * 
 */
public class QuickSortV2 {

	List<Individual> population;
	private Individual array[];
	private int length;
	private boolean sortByTIC;
	private boolean sortByTIM;
	private boolean sortByTIP;
	private boolean sortByLOC;
	private boolean sortByCrowdedDistance;

	public List<Individual> sort(List<Individual> population,
			boolean sortByTIC, boolean sortByTIM, boolean sortByTIP,
			boolean sortByLOC, boolean sortByCrowdedDistance) {
		if (population == null || population.size() == 0) {
			System.err
					.println("QuickSortV2.sort population == null || population.size() == 0");
			return null;
		}
		if (sortByTIC == false && sortByTIM == false && sortByTIP == false
				&& sortByLOC == false && sortByCrowdedDistance == false) {
			System.err
					.println("sortByTIC, sortByTIP, sortByLOC and sortByCrowdedDistance can't be false");
			return null;
		}
		this.population = population;
		length = population.size();
		this.sortByTIC = sortByTIC;
		this.sortByTIM = sortByTIM;
		this.sortByTIP = sortByTIP;
		this.sortByLOC = sortByLOC;
		this.sortByCrowdedDistance = sortByCrowdedDistance;
		this.array = this.toArray(population, length);
		quickSort(0, length - 1);
		return this.toList(array, length);
	}

	private Individual[] toArray(List<Individual> population, int length) {
		Individual[] array = new Individual[length];
		Individual individual;
		for (int i = 0; i < length; i++) {
			individual = population.get(i);
			array[i] = individual;
		}
		return array;
	}

	private List<Individual> toList(Individual[] array, int length) {
		List<Individual> list = new ArrayList<Individual>();
		Individual individual;
		// String sortBy = sortByTIC ? "TIC" : sortByTIP ? "TIP"
		// : "Crowded distance";
		// System.err.println("\t---- Result " + sortBy + " ----");
		for (int i = 0; i < length; i++) {
			individual = array[i];
			list.add(individual);
			// System.err.println("\t" + i + " - " + individual);
		}
		return list;
	}

	private void quickSort(int lowerIndex, int higherIndex) {
		int i = lowerIndex;
		int j = higherIndex;
		// calculate pivot number, I am taking pivot as middle index number
		int pivotIndex = lowerIndex + ((higherIndex - lowerIndex) / 2);
		// Divide into two arrays
		while (i <= j) {
			/**
			 * In each iteration, we will identify a number from left side which
			 * is greater then the pivot value, and also we will identify a
			 * number from right side which is less then the pivot value. Once
			 * the search is done, then we exchange both numbers.
			 */
			if (sortByTIC || sortByTIM || sortByTIP || sortByLOC) {
				while (isGreater(pivotIndex, i)) {
					i++;
					if (i == array.length) {
						break;
					}
				}
				while (isLess(pivotIndex, j)) {
					j--;
					if (j == 0) {
						break;
					}
				}
			} else if (sortByCrowdedDistance) {
				while (isLess(pivotIndex, i)) {
					i++;
					if (i == array.length) {
						break;
					}
				}
				while (isGreater(pivotIndex, j)) {
					j--;
					if (j == 0) {
						break;
					}
				}
			}
			if (i <= j) {
				exchangeNumbers(i, j);
				// move index to next position on both sides
				i++;
				j--;
			}
		}
		// call quickSort() method recursively
		if (lowerIndex < j) {
			quickSort(lowerIndex, j);
		}
		if (i < higherIndex) {
			quickSort(i, higherIndex);
		}
	}

	private boolean isLess(int pivotIndex, int compIndex) {
		// ordenar capacidad
		Individual pivot = array[pivotIndex];
		Individual comp = array[compIndex];
		if (sortByTIC) {
			if (pivot.compareByTIC(comp) < 0) {
				return true;
			} else if (pivot.compareByTIC(comp) > 0) {
				return false;
			}
			return false;
		}
		if (sortByTIM) {
			if (pivot.compareByTIM(comp) < 0) {
				return true;
			} else if (pivot.compareByTIM(comp) > 0) {
				return false;
			}
			return false;
		}
		if (sortByTIP) {
			if (pivot.compareByTIP(comp) < 0) {
				return true;
			} else if (pivot.compareByTIP(comp) > 0) {
				return false;
			}
			return false;
		}
		if (sortByLOC) {
			if (pivot.getMinLoadIndex() < comp.getMinLoadIndex()) {
				return true;
			} else if (pivot.getMinLoadIndex() > comp.getMinLoadIndex()) {
				return false;
			}
			return false;
		}
		if (sortByCrowdedDistance) {
			if (pivot.compareByCrowdedDistance(comp) < 0) {
				return true;
			} else if (pivot.compareByCrowdedDistance(comp) > 0) {
				return false;
			}
			return false;
		}
		return true;
	}

	private boolean isGreater(int pivotIndex, int compIndex) {
		// ordenar capacidad
		Individual pivot = array[pivotIndex];
		Individual comp = array[compIndex];
		if (sortByTIC) {
			if (pivot.compareByTIC(comp) > 0) {
				return true;
			} else if (pivot.compareByTIC(comp) < 0) {
				return false;
			}
			return false;
		}
		if (sortByTIM) {
			if (pivot.compareByTIM(comp) > 0) {
				return true;
			} else if (pivot.compareByTIM(comp) < 0) {
				return false;
			}
			return false;
		}
		if (sortByTIP) {
			if (pivot.compareByTIP(comp) > 0) {
				return true;
			} else if (pivot.compareByTIP(comp) < 0) {
				return false;
			}
			return false;
		}
		if (sortByLOC) {
			if (pivot.getMinLoadIndex() > comp.getMinLoadIndex()) {
				return true;
			} else if (pivot.getMinLoadIndex() < comp.getMinLoadIndex()) {
				return false;
			}
			return false;
		}
		if (sortByCrowdedDistance) {
			if (pivot.compareByCrowdedDistance(comp) > 0) {
				return true;
			} else if (pivot.compareByCrowdedDistance(comp) < 0) {
				return false;
			}
			return false;
		}
		return false;
	}

	private void exchangeNumbers(int i, int j) {
		Individual indexTemp = array[i];
		array[i] = array[j];
		array[j] = indexTemp;
	}

}
