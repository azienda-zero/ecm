package it.tredi.ecm.web.bean;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScadenzeEventoWrapper {

	@DateTimeFormat (pattern = "dd/MM/yyyy")
	private LocalDate dataScadenzaPagamento;
	@DateTimeFormat (pattern = "dd/MM/yyyy")
	private LocalDate dataScadenzaRendicontazione;

	private boolean submitScadenzeError = false;

	private String returnLink;
}
