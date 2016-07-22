package it.tredi.ecm.dao.entity;

import java.util.Objects;

import javax.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class FieldEditabile extends Field {
	@Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FieldEditabile entitapiatta = (FieldEditabile) o;
        return Objects.equals(id, entitapiatta.id);
    }
}
