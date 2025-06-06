/**
 * 
 */
package com.github.gtoison.sample;

import jakarta.persistence.Transient;

/**
 * @author gtoison
 */
public interface Nicknamed extends Named {

	@Override
	default String getName() {
		return "My nickname is " + getNickname();
	}

	@Transient
	default String getNickname() {
		return getName() + "y";
	}
}
