package broker.mo.util;

import java.math.BigDecimal;

/**
 * 
 * @author Lino Chamorro
 * 
 */
public class DCSUtil {

	public static int getRandomInt() {
		return (int) (Math.random() * DCSConstants.MAX_RANDOM);
	}

	public static int getRandom(int n) {
		return (int) (DCSUtil.getRandomInt() % n);
	}

	public static int getMax(BigDecimal[] p) {
		int max = 0;
		int j = -1;
		for (int i = 0; i < p.length; i++) {
			if (p[i].intValue() > max) {
				max = p[i].intValue();
				j = i;
			}
		}
		return j;
	}

	public static int getMin(BigDecimal[] p) {
		int min = 999999;
		int j = -1;
		for (int i = 0; i < p.length; i++) {
			if (p[i].intValue() < min) {
				min = p[i].intValue();
				j = i;
			}
		}
		return j;
	}

	public static double getRandomU() {
		double[] u = { 0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9 };
		int index = getRandom(u.length);
		return u[index];
	}

	public static double getRandomR() {
		double[] r = { 0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9 };
		int index = getRandom(r.length);
		return r[index];
	}

	public static BigDecimal getMinPercent(BigDecimal n, BigDecimal percent) {
		BigDecimal tmp = (n.multiply(percent)).divide(new BigDecimal(100));
		BigDecimal minPercent = n.subtract(tmp);
		return minPercent;
	}

	public static BigDecimal getMaxPercent(BigDecimal n, BigDecimal percent) {
		BigDecimal tmp = (n.multiply(percent)).divide(new BigDecimal(100));
		BigDecimal maxPercent = n.add(tmp);
		return maxPercent;
	}

}
