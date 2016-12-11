package it.tredi.ecm.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.tredi.ecm.dao.entity.Accreditamento;
import it.tredi.ecm.dao.entity.AlertEmail;
import it.tredi.ecm.dao.entity.Provider;
import it.tredi.ecm.dao.entity.QuotaAnnuale;
import it.tredi.ecm.dao.enumlist.AlertTipoEnum;
import it.tredi.ecm.dao.repository.AlertEmailRepository;
import it.tredi.ecm.utils.Utils;

@Service
public class AlertEmailServiceImpl implements AlertEmailService {
	private static Logger LOGGER = LoggerFactory.getLogger(AlertEmailServiceImpl.class);

	@Autowired private AlertEmailRepository alertMailRepository;
	@Autowired private EmailService emailService;

	@Override
	public Set<AlertEmail> getAll() {
		LOGGER.info("Recupero tutti gli Alert Registrati");
		return alertMailRepository.findAll();
	}

	@Override
	public Set<AlertEmail> getAllNotInviati() {
		LOGGER.info("Recupero tutti gli Alert Da Inviare");
		return alertMailRepository.findAllByInviatoFalse();
	}

	private boolean checkIfExistForProvider(AlertTipoEnum tipo, Long providerId, LocalDateTime dataScadenza) {
		LOGGER.info("Verifica se Alert già registrato");
		AlertEmail alert = alertMailRepository.findByTipoAndProviderIdAndDataScadenza(tipo, providerId, dataScadenza);
		if(alert != null)
			return true;
		return false;
	}

	@Override
	public boolean checkIfExistForEvento(AlertTipoEnum tipo, Long eventoId, LocalDateTime dataScadenza) {
		LOGGER.info("Verifica se Alert già registrato");
		AlertEmail alert = alertMailRepository.findByTipoAndEventoIdAndDataScadenza(tipo, eventoId, dataScadenza);
		if(alert != null)
			return true;
		return false;
	}

	@Override
	public void creaAlertForProvider(Accreditamento accreditamento) {
		LOGGER.info("Creazione Alert per Accreditamento");
		if(accreditamento.isPreavvisoRigetto() || accreditamento.isIntegrazione()){
			LocalDateTime dataScadenza = Utils.convertLocalDateToLocalDateTime(LocalDate.now().plusDays(accreditamento.getGiorniIntegrazione()));
			dataScadenza = dataScadenza.minusDays(2);

			AlertTipoEnum tipo;
			if(accreditamento.isStandard()){
				if(accreditamento.isPreavvisoRigetto()){
					tipo = AlertTipoEnum.SCADENZA_REINVIO_INTEGRAZIONI_PREAVVISO_DI_RIGETTO_ACCREDITAMENTO_STANDARD;
				}else{
					tipo = AlertTipoEnum.SCADENZA_REINVIO_INTEGRAZIONI_ACCREDITAMENTO_STANDARD;
				}
			}else{
				if(accreditamento.isPreavvisoRigetto()){
					tipo = AlertTipoEnum.SCADENZA_REINVIO_INTEGRAZIONI_PREAVVISO_DI_RIGETTO_ACCREDITAMENTO_PROVVISORIO;
				}else{
					tipo = AlertTipoEnum.SCADENZA_REINVIO_INTEGRAZIONI_ACCREDITAMENTO_PROVVISORIO;
				}
			}

			if(!checkIfExistForProvider(tipo, accreditamento.getProvider().getId(), dataScadenza))
				creaAlertForProvider(tipo, accreditamento.getProvider(), dataScadenza);
		}else if(accreditamento.isAccreditato()){
			LocalDateTime dataScadenza = Utils.convertLocalDateToLocalDateTime(accreditamento.getDataFineAccreditamento());
			dataScadenza = dataScadenza.minusDays(90);

			AlertTipoEnum tipo;
			if(accreditamento.isStandard()){
				tipo = AlertTipoEnum.SCADENZA_ACCREDITAMENTO_STANDARD;
			}else{
				tipo = AlertTipoEnum.SCADENZA_ACCREDITAMENTO_PROVVISORIO;
			}

			if(!checkIfExistForProvider(tipo, accreditamento.getProvider().getId(), dataScadenza))
				creaAlertForProvider(tipo, accreditamento.getProvider(), dataScadenza);
		}
	}

	@Override
	public void creaAlertContributoAnnuoForProvider(QuotaAnnuale quota) {
		LOGGER.info("Creazione Alert per QuotaAnnuale");
		LocalDateTime dataScadenza = Utils.convertLocalDateToLocalDateTime(quota.getPagamento().getDataScadenzaPagamento());
		dataScadenza = dataScadenza.minusDays(10);
		creaAlertForProvider(AlertTipoEnum.SCADENZA_QUOTA_ANNUALE, quota.getProvider(), dataScadenza);
	}

	private void creaAlertForProvider(AlertTipoEnum tipo, Provider provider, LocalDateTime dataScadenza) {
		LOGGER.info("Creazione Alert per " + tipo);
		AlertEmail alert = new AlertEmail();
		alert.setProvider(provider);
		alert.setDataScadenza(dataScadenza);
		alert.setTipo(tipo);

		Set<String> destinatari = new HashSet<String>();

		if(provider.getLegaleRappresentante() != null)
			destinatari.add(provider.getLegaleRappresentante().getAnagrafica().getEmail());
		if(provider.getDelegatoLegaleRappresentante() != null)
			destinatari.add(provider.getDelegatoLegaleRappresentante().getAnagrafica().getEmail());

		alert.setDestinatari(destinatari);
		save(alert);
	}

	@Override
	@Transactional
	public void save(AlertEmail alert) {
		LOGGER.info("Salvataggio Alert Mail");
		alertMailRepository.save(alert);
	}

	//Metodo da richiamare nel Task
	@Override
	public void inviaAlertsEmail() throws Exception{
		Set<AlertEmail> alerts = alertMailRepository.findAllByDataInvioIsNullAndDataScadenzaBefore(Timestamp.valueOf(LocalDate.now().plusDays(1).atStartOfDay()).toLocalDateTime());

		for(AlertEmail alert :  alerts){
			try{
				if(alert.getTipo() == AlertTipoEnum.SCADENZA_REINVIO_INTEGRAZIONI_ACCREDITAMENTO_PROVVISORIO ||	alert.getTipo() == AlertTipoEnum.SCADENZA_REINVIO_INTEGRAZIONI_PREAVVISO_DI_RIGETTO_ACCREDITAMENTO_PROVVISORIO ||
					alert.getTipo() == AlertTipoEnum.SCADENZA_REINVIO_INTEGRAZIONI_ACCREDITAMENTO_STANDARD || alert.getTipo() == AlertTipoEnum.SCADENZA_REINVIO_INTEGRAZIONI_PREAVVISO_DI_RIGETTO_ACCREDITAMENTO_STANDARD )
				{
					emailService.inviaAlertScadenzaReInvioIntegrazioneAccreditamento(alert);
					alert.setDataInvio(LocalDateTime.now());
					save(alert);
				}else if(alert.getTipo() == AlertTipoEnum.SCADENZA_ACCREDITAMENTO_PROVVISORIO || alert.getTipo() == AlertTipoEnum.SCADENZA_ACCREDITAMENTO_STANDARD){
					emailService.inviaAlertScadenzaAccreditamento(alert);
					alert.setDataInvio(LocalDateTime.now());
					save(alert);
				}else if(alert.getTipo() == AlertTipoEnum.SCADENZA_QUOTA_ANNUALE){
					emailService.inviaAlertScadenzaPagamento(alert);
					alert.setDataInvio(LocalDateTime.now());
					save(alert);
				}

//				case SCADENZA_COMPILAZIONE_PFA: break;
//				case SCADENZA_RELAZIONE_ANNUALE: break;
//				case SCADENZA_PAGAMENTO_E_RENDICONTAZIONE_EVENTO: break;
//				case SCADENZA_COMPILAZIONE_DOMANDA_ACCREDITAMENTO_STANDARD: break;
//				default : break;
//				}
			}catch (Exception ex){
				LOGGER.error(ex.getMessage(),ex);
			}
		}
	}
}
