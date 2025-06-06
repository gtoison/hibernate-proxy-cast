/**
 * 
 */
package com.github.gtoison.caster;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

/**
 * @author Guillaume Toison
 */
public final class HibernateProxyCaster {
	private static final Logger LOGGER = LoggerFactory.getLogger(HibernateProxyCaster.class);

	private static Level logLevel;

	private HibernateProxyCaster() {
	}

	public static boolean proxyInstanceof(Object o, Class<?> c) {
		if (o == null) {
			return false;
		} 

		boolean proxyInstanceof = c.isAssignableFrom(Hibernate.getClassLazy(o));

		if (logLevel != null) {
			boolean defaultInstanceof = c.isAssignableFrom(Hibernate.getClassLazy(o));
			if (defaultInstanceof != proxyInstanceof) {
				logAtLevel("Instanceof differed because {} was a proxy of class {} while testing for class {}", o, o.getClass(), c);
			}
		}

		return proxyInstanceof;
	}

	@SuppressWarnings("unchecked")
	public static <T> T proxyCast(Object o, Class<T> c) {
		if (c.isInstance(o)) {
			return (T) o;
		}

		if (o == null) {
			return null;
		}

		T unproxy = (T) Hibernate.unproxy(o);

		logAtLevel("Unproxying {} to cast as {}", o, c);

		return unproxy;
	}

	private static void logAtLevel(String format, Object arg1, Object arg2) {
		if (logLevel == null) {
			return;
		}

		switch (logLevel) {
			case TRACE:
				LOGGER.trace(format, arg1, arg2);
				break;
			case DEBUG:
				LOGGER.debug(format, arg1, arg2);
				break;
			case INFO:
				LOGGER.info(format, arg1, arg2);
				break;
			case WARN:
				LOGGER.warn(format, arg1, arg2);
				break;
			case ERROR:
				LOGGER.error(format, arg1, arg2);
				break;
		}
	}

	private static void logAtLevel(String format, Object arg1, Object arg2, Object arg3) {
		if (logLevel == null) {
			return;
		}

		switch (logLevel) {
			case TRACE:
				LOGGER.trace(format, arg1, arg2, arg3);
				break;
			case DEBUG:
				LOGGER.debug(format, arg1, arg2, arg3);
				break;
			case INFO:
				LOGGER.info(format, arg1, arg2, arg3);
				break;
			case WARN:
				LOGGER.warn(format, arg1, arg2, arg3);
				break;
			case ERROR:
				LOGGER.error(format, arg1, arg2, arg3);
				break;
		}
	}

	public static void setLogLevel(Level logLevel) {
		HibernateProxyCaster.logLevel = logLevel;
	}
}
