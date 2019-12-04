package no.ssb.helidon.registry;

import no.ssb.helidon.Registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HelidonRegistry implements Registry {

    final Map<ClassAndName, RegisteredInstance> services = new ConcurrentHashMap<>();

    public <T> HelidonRegistry add(Class<T> clazz, T instance) {
        services.put(new ClassAndName(clazz), new RegisteredInstance(instance));
        return this;
    }

    public <T> HelidonRegistry add(Class<T> clazz, String name, T instance) {
        services.put(new ClassAndName(clazz, name), new RegisteredInstance(instance));
        return this;
    }

    @Override
    public <T> T remove(Class<T> clazz) {
        return (T) services.remove(new ClassAndName(clazz)).instance;
    }

    @Override
    public <T> T remove(Class<T> clazz, String name) {
        return (T) services.remove(new ClassAndName(clazz, name)).instance;
    }

    public <T> T get(Class<T> clazz) {
        return (T) services.get(new ClassAndName(clazz)).instance;
    }

    public <T> T get(Class<T> clazz, String name) {
        return (T) services.get(new ClassAndName(clazz, name)).instance;
    }
}
