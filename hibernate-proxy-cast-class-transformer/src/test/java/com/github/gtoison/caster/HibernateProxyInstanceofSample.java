/**
 * 
 */
package com.github.gtoison.caster;

/**
 * 
 */
public class HibernateProxyInstanceofSample {

	public int instanceofTest(Object o) {
		if (o instanceof String) {
			return 1;
		} else {
			return 2;
		}
	}
}
