package it.tredi.ecm.web.validator;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import it.tredi.ecm.dao.entity.File;
import it.tredi.ecm.dao.enumlist.FileEnum;
import it.tredi.ecm.service.ProviderService;
import it.tredi.ecm.service.ProviderServiceImpl;
import it.tredi.ecm.service.bean.EcmProperties;
import it.tredi.ecm.service.bean.VerificaFirmaDigitale;
import it.tredi.ecm.utils.Utils;

@Component
public class FileValidator {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileValidator.class);

	@Autowired private EcmProperties ecmProperties;
	@Autowired private MessageSource messageSource;
	@Autowired private ProviderService providerService;
	
	public void validate(Object target, Errors errors, String prefix, Long providerId) throws Exception{
		validateData(target, errors, prefix);
		validateFirma(target, errors, prefix, providerId);
	}
	
	public void validateData(Object target, Errors errors, String prefix) {
		LOGGER.info(Utils.getLogMessage("Validazione File"));
		File file = (File)target;
		if(file == null || file.getNomeFile().isEmpty() || file.getData().length == 0){
			errors.rejectValue(prefix, "error.empty");
		}else{
			
			if(file.getData().length > ecmProperties.getMultipartMaxFileSize()){
				errors.rejectValue(prefix, "error.maxFileSize", new Object[]{String.valueOf(ecmProperties.getMultipartMaxFileSize()/(1024*1024) )},"");
			}
		}
	}
	
	public void validateFirma(Object target, Errors errors, String prefix, Long providerId) throws Exception{
		File file = (File)target;
		if(!(file == null || file.getNomeFile().isEmpty() || file.getData().length == 0)){
			
			//se il cf della firma non appartiene al legale rappresentane o al delegato del legale rappresentante, allora non è valido
			if(!validateFirmaCF(file, providerId))
				errors.rejectValue(prefix, "error.codiceFiscale.firmatario");
		}
	}
	
	public boolean validateFirmaCF(Object target, Long providerId) throws Exception{
		File file = (File)target;
		if(!(file == null || file.getNomeFile().isEmpty() || file.getData().length == 0)){
			
			VerificaFirmaDigitale verificaFirmaDigitale = new VerificaFirmaDigitale(file.getNomeFile(), file.getData());
			String cfLegaleRappresentante = providerService.getCodiceFiscaleLegaleRappresentantePerVerificaFirmaDigitale(providerId);
			String cfDelegatoLegaleRappresentante = providerService.getCodiceFiscaleDelegatoLegaleRappresentantePerVerificaFirmaDigitale(providerId);
			
			//se il cf della firma non appartiene al legale rappresentane o al delegato del legale rappresentante, allora non è valido
			if(cfLegaleRappresentante.equalsIgnoreCase(verificaFirmaDigitale.getLastSignerCF()) || cfDelegatoLegaleRappresentante.equalsIgnoreCase(verificaFirmaDigitale.getLastSignerCF()))
				return true;
			
			return false;
		}
		
		return false;
	}
	
	public String validate(Object target, String contentType) throws Exception{
		LOGGER.info(Utils.getLogMessage("Validazione File AJAX Upload"));
		File file = (File)target;
		String error = "";
		if(file == null || file.getNomeFile().isEmpty() || file.getData().length == 0){
			error = messageSource.getMessage("error.empty", null, Locale.getDefault());
		}else{
			//validazione file xml/csv/xml.p7m/xml.zip.p7m
			if(file.getTipo() == FileEnum.FILE_REPORT_PARTECIPANTI) {
//				if(!(contentType.equalsIgnoreCase("application/xml") ||
//						contentType.equalsIgnoreCase("text/xml") ||
//						contentType.equalsIgnoreCase("application/pkcs7-mime") ||
//						contentType.equalsIgnoreCase("application/x-pkcs7-mime") ||
//						contentType.equalsIgnoreCase("text/csv")))
//					error = messageSource.getMessage("error.formatNonAcceptedXML", new Object[]{}, Locale.getDefault());
			}
			else if(file.getTipo() == FileEnum.FILE_EVENTI_PIANO_FORMATIVO) {
				if (!file.getNomeFile().toUpperCase().endsWith(".CSV"))
					error = messageSource.getMessage("error.formatNonAcceptedCSV", new Object[]{}, Locale.getDefault());
			}
			//validazione file pdf/pdf.p7m
			else {
				if(!(contentType.equalsIgnoreCase("application/pdf") || contentType.equalsIgnoreCase("application/pkcs7-mime")))
					error = messageSource.getMessage("error.formatNonAccepted", new Object[]{}, Locale.getDefault());
			}
			if(file.getData().length > ecmProperties.getMultipartMaxFileSize()){
				error = messageSource.getMessage("error.maxFileSize", new Object[]{String.valueOf(ecmProperties.getMultipartMaxFileSize()/(1024*1024) )}, Locale.getDefault());
			}
		}
		return error;
	}

	public void validateWithCondition(Object target, Errors errors, String prefix, Boolean condition, Long providerId) throws Exception{
		LOGGER.info(Utils.getLogMessage("Validazione File required su condizione"));
		File file = (File)target;
		if(condition == true)
			validate(target, errors, prefix, providerId);
		else {
			if(file != null && !file.getNomeFile().isEmpty())
				validate(file, errors, prefix, providerId);
		}

	}
}
