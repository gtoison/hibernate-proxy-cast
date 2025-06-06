/**
 * 
 */
package com.github.gtoison.sample;

/**
 * @author gtoison
 */
public interface DualWinged extends Winged {
	default String flagDualWinged() {
		return "flap flap";
	}
}
