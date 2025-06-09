package com.github.gtoison.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

import com.github.gtoison.caster.HibernateProxyCaster;
import com.github.gtoison.sample.Animal;
import com.github.gtoison.sample.AnimalChecker;
import com.github.gtoison.sample.Human;
import com.github.gtoison.sample.Insect;
import com.github.gtoison.sample.Ladybird;
import com.github.gtoison.sample.Mollusc;
import com.github.gtoison.sample.WingedInsect;

/**
 * @author Guillaume Toison
 */
@DomainModel(annotatedClasses = { 
		Animal.class, 
		Mollusc.class, 
		Human.class, 
		Insect.class,
		WingedInsect.class,
		Ladybird.class,
})
@SessionFactory
class ProxyCastTest {
	@BeforeAll
	static void updateLogLevel(SessionFactoryScope scope) {
		HibernateProxyCaster.setLogLevel(Level.INFO);

		scope.inTransaction(session -> {
			Mollusc snail = new Mollusc(1, "Snail", true);
			Human human = new Human(2, "Human", snail);
			Ladybird ladybird = new Ladybird(3, "Ladybird");

			session.persist(snail);
			session.persist(human);
			session.persist(ladybird);
		});
	}

	@Test
	void instanceofTest(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			Animal animal = session.getReference(Animal.class, 1);
			AnimalChecker checker = new AnimalChecker();

			assertTrue(checker.isMollusc(animal));
		});
	}

	@Test
	void castTest(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			Human human = session.getReference(Human.class, 2);

			assertTrue(human.getPetMollusc() instanceof Mollusc);
		});
	}

	@Test
	void patternMatchingTest(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			Human human = session.getReference(Human.class, 2);
			AnimalChecker checker = new AnimalChecker();

			assertNotNull(checker.getHumanPetAsMollusc(human));
		});
	}
	
	@Test
	void covarCastTest(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			Human human = session.getReference(Human.class, 2);
			AnimalChecker checker = new AnimalChecker();

			assertTrue(checker.getHumanPetAsMolluscWithCovarPetCast(human) instanceof Mollusc);
		});
	}

	@Test
	void defaultInterfaceTest(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			WingedInsect wingedInsect = session.getReference(WingedInsect.class, 3);
			AnimalChecker checker = new AnimalChecker();

			assertEquals("I'm a ladybird: flap", wingedInsect.flapWings());
			assertEquals("I'm a ladybird: flap", checker.wingedFlapWings(wingedInsect));
			assertEquals("I'm a ladybird: flap", checker.wingedInsectFlapWings(wingedInsect));
		});

		scope.inTransaction(session -> {
			Ladybird wingedInsect = session.getReference(Ladybird.class, 3);
			AnimalChecker checker = new AnimalChecker();

			assertEquals("I'm a ladybird: flap", wingedInsect.flapWings());
			assertEquals("I'm a ladybird: flap", checker.wingedFlapWings(wingedInsect));
			assertEquals("I'm a ladybird: flap", checker.wingedInsectFlapWings(wingedInsect));
		});

		scope.inTransaction(session -> {
			Animal human = session.getReference(Animal.class, 2);
			AnimalChecker checker = new AnimalChecker();

			assertEquals("Human and my pet is Snail", checker.nickname(human));
			assertEquals("Human", checker.nickNamedName(human));
			assertEquals("Human", checker.name(human));
		});
	}
}
