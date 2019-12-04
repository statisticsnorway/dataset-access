package no.ssb.helidon;

import no.ssb.helidon.registry.HelidonRegistry;

public interface Registry {

    static Registry create() {
        return new HelidonRegistry();
    }

    <T> Registry add(Class<T> clazz, T instance);

    <T> Registry add(Class<T> clazz, String name, T instance);

    <T> T remove(Class<T> clazz);

    <T> T remove(Class<T> clazz, String name);

    <T> T get(Class<T> clazz);

    <T> T get(Class<T> clazz, String name);
}
