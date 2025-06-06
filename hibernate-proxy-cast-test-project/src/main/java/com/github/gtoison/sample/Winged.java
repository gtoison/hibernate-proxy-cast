/**
 * 
 */
package com.github.gtoison.sample;

/**
 * @author gtoison
 */
public interface Winged {
	default String flapWings() {
		return "flap";
	}
}
