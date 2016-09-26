package it.tredi.ecm.web.validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import it.tredi.ecm.dao.entity.Comunicazione;
import it.tredi.ecm.dao.entity.ComunicazioneResponse;
import it.tredi.ecm.utils.Utils;

@Component
public class ComunicazioneValidator {
	private static final Logger LOGGER = LoggerFactory.getLogger(ComunicazioneValidator.class);

	public void validate(Object target, Errors errors, String prefix){
		LOGGER.info(Utils.getLogMessage("Validazione Comunicazione"));
		Comunicazione comunicazione= (Comunicazione)target;

		if(comunicazione.getAmbito() == null)
			errors.rejectValue(prefix + "ambito", "error.empty");
		if(comunicazione.getTipologia() == null)
			errors.rejectValue(prefix + "tipologia", "error.empty");
		if(comunicazione.getOggetto() == null || comunicazione.getOggetto().isEmpty())
			errors.rejectValue(prefix + "oggetto", "error.empty");
		if(comunicazione.getMittente().isSegreteria() && (comunicazione.getDestinatari() == null || comunicazione.getDestinatari().isEmpty()))
			errors.rejectValue(prefix + "destinatari", "error.empty");

		Utils.logDebugErrorFields(LOGGER, errors);
	}

	public void validateReply(Object target, Errors errors, String prefix){
		LOGGER.info(Utils.getLogMessage("Validazione Comunicazione"));
		ComunicazioneResponse response = (ComunicazioneResponse)target;

		if(response.getMessaggio() == null || response.getMessaggio().isEmpty())
			errors.rejectValue(prefix + "messaggio", "error.empty");

		Utils.logDebugErrorFields(LOGGER, errors);
	}

}