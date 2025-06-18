Hibernate proxy caster modifies classes to replace the casts and instanceof checks for objects that are potentially instances of the "wrong class".

# Usage

The proxy caster needs to know which classes and interfaces might be Hibernate proxies, it uses a Jandex index for that.
You can use the maven plugin to build the index:

```xml
	<build>
		<plugins>
			<plugin>
				<groupId>io.smallrye</groupId>
				<artifactId>jandex-maven-plugin</artifactId>
				<version>${jandex.version}</version>
				<executions>
					<execution>
						<id>make-index</id>
						<goals>
							<goal>jandex</goal>
						</goals>
						<phase>process-classes</phase>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>
```

### Runtime dependency

A small library is needed as a runtime dependency:

```xml
		<dependency>
			<groupId>com.github.gtoison</groupId>
			<artifactId>hibernate-proxy-caster</artifactId>
			<version>0.0.1-SNAPSHOT</version>
		</dependency>
```

### Agent base transformation

The proxy caster can modify classes at runtime with an agent. Add the following option to the JVM startup parameters:

```
-javaagent:<PATH_TO_AGENT>/hibernate-proxy-cast-agent-0.0.1-SNAPSHOT.jar

```

### Built time transformation

Alternatively the proxy caster can modify the bytecode from the `.class` files with a maven plugin:

```xml
	<build>
		<plugins>
			<!-- Also add the Jandex plugin here -->
			<plugin>
				<groupId>com.github.gtoison</groupId>
				<artifactId>hibernate-proxy-caster-maven-plugin</artifactId>
				<version>0.0.1-SNAPSHOT</version>
				<executions>
					<execution>
						<id>transform-classes</id>
						<goals>
							<goal>transform</goal>
						</goals>
						<phase>process-classes</phase>
					</execution>
				</executions>
			</plugin>
		</plugins>
		</plugins>
	</build>
```

# The problem

Consider the following JPA model:

```java
@Entity
class Animal {
}

@Entity
class Bug extends Animal {
}

@Entity
class WingedBug extends Bug implements Winged {
}

interface Winged {
	void flapWings();
}

@Entity
class Human extends Animal {
	@ManyToOne
	Animal pet;
}
```

Hibernate uses proxies to load entities lazily, this works by dynamically generating a class that extends the class you have written. 

If you get an uninitialized instance of this proxy, essentially all its fields will be null except the ID because Hibernate has not yet hit the database. Now the first time you will call a method on this proxy, it will realize that it is not initialized and it will query the database to load it's attributes. This is possible because the dynamically generated class overrides the base class's methods and adds this initialized/uninitialized check.

Now the trouble comes when combining lazy loading and inheritance: in case Hibernate does not know the actual class of an entity, it might generate a proxy of the _wrong_ class.
For instance, assuming that `Animal#1` is a `Human`, the following code will not work as expected:

```java
Animal a = session.getReference(Animal.class, 1);
if (a instanceof Human h) {
  h.thinkAboutDeathAndTheUniverse();
}
```
This won't work because `a` will be an instance of a generated class (something like `Animal$HibernateProxy$Bx1ZccZd`), not an instance of `Human`

# Hibernate's built-in solution

Up until Hibernate 6, a workaround for this was to annotate the problematic entities with `@Proxy(lazy=false)`, this disabled lazy loading and the entities had the _right_ class. This was however not recommended because it had negative performance implication (lazy loading is generally faster than eager loading).
The `@Proxy` annotation was deprecated in Hibernate 6 and removed in Hibernate 7.

In Hibernate 6.6 a new `@ConcreteProxy` was added, it guarantees that Hibernate will produce proxies of the correct subtype. This however comes with a performance hit, because Hibernate somehow needs to retrieve that type from the database.

It is also possible to get the actual class from Hibernate with `Hibernate.getClass(animal)` and then one can get the object of the expected class with `Human h = (Human) Hibernate.unproxy(animal)`.
The equivalent of the idiomatic code:

```java
if (animal instanceof Human human) {
  human.thinkAboutDeathAndTheUniverse();
}
```

Would be:

```java
if (Human.class.isAssignableFrom(Hibernate.getClass(animal)) {
  Human human = (Human) Hibernate.unproxy(animal);
  human.thinkAboutDeathAndTheUniverse();
}
```

Note that in case `animal` was a proxy, then `human != animal`: unproxying breaks `==` equality because the unproxied object is a different instance.
