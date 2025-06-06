/**
 * 
 */
package com.github.gtoison.caster.agent;

import org.junit.jupiter.api.Test;

/**
 * 
 */
class HibernateProxyCasterAgentTest {
	
	@Test
	void test() {
		System.out.println("test");
	}

	@Test
	void instanceofTest() {
		Object x = 1;
		if (x instanceof Integer) {
			Integer i = (Integer) x;
			System.out.println("cast updated");
		} else {
			System.out.println("cast not updated");
		}
		
		x = null;
		if (x instanceof String) {
			System.out.println("cast updated");
		} else {
			System.out.println("cast not updated");
		}
	}
	
	public static void main(String[] args) {
		new HibernateProxyCasterAgentTest().instanceofTest();
	}
}
