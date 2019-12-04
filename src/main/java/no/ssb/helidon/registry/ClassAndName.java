package no.ssb.helidon.registry;

import java.util.Objects;

class ClassAndName {
    final Class clazz;
    final String name;

    ClassAndName(Class clazz, String name) {
        this.clazz = clazz;
        this.name = name;
    }

    ClassAndName(Class clazz) {
        this.clazz = clazz;
        this.name = "_default_";
    }

    @Override
    public String toString() {
        return "ClassAndName{" +
                "clazz=" + clazz +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassAndName that = (ClassAndName) o;
        return clazz.equals(that.clazz) &&
                name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clazz, name);
    }
}
