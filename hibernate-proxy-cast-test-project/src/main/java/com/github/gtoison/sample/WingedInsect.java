/**
 * 
 */
package com.github.gtoison.sample;

import jakarta.persistence.Entity;

/**
 * @author gtoison
 */
@Entity
public class WingedInsect extends Insect implements Winged {

	public WingedInsect() {
	}

	public WingedInsect(Integer id, String name) {
		super(id, name);
	}
}
