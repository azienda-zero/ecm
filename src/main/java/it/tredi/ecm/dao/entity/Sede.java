package it.tredi.ecm.dao.entity;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.javers.core.metamodel.annotation.DiffIgnore;
import org.javers.core.metamodel.annotation.TypeName;

import com.fasterxml.jackson.annotation.JsonView;

import lombok.Getter;
import lombok.Setter;

@TypeName("Sede")
@Entity
@Table(name="sede")
@Getter
@Setter
public class Sede extends BaseEntityDefaultId{
	private String provincia;
	private String comune;
	private String indirizzo;
	private String cap;
	private String telefono;
	private String altroTelefono;
	private String fax;
	private String email;

	//boolean controllo tipologia sede (default operativa)
	private boolean sedeLegale = false;
	private boolean sedeOperativa = true;

	@DiffIgnore
	@ManyToOne @JoinColumn(name = "provider_id")
	private Provider provider;

	//se true significa che non è stato ancora validato dalla segreteria
	@DiffIgnore
	@JsonView(JsonViewModel.Integrazione.class)
	private boolean dirty = false;

	@Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Sede entitapiatta = (Sede) o;
        return Objects.equals(id, entitapiatta.id);
    }

	public String getAddressName() {
		return (this.indirizzo + " (" + this.comune + ")");
	}

	public String getAddressNameFull() {
		return (this.indirizzo + ", " + this.comune + " (" + this.provincia + ")");
	}
}
