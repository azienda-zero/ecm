package it.tredi.ecm.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.Authenticator;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.tredi.ecm.dao.entity.Evento;
import it.tredi.ecm.dao.entity.PagDovutiLog;
import it.tredi.ecm.dao.entity.PagPagatiLog;
import it.tredi.ecm.dao.entity.Pagamento;
import it.tredi.ecm.dao.entity.Provider;
import it.tredi.ecm.dao.repository.EventoRepository;
import it.tredi.ecm.dao.repository.PagDovutiLogRepository;
import it.tredi.ecm.dao.repository.PagPagatiLogRepository;
import it.tredi.ecm.dao.repository.PagamentoRepository;
import it.tredi.ecm.utils.HttpAuthenticateProxy;
import it.veneto.regione.pagamenti.ente.FaultBean;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovuti;
import it.veneto.regione.pagamenti.ente.PaaSILInviaDovutiRisposta;
import it.veneto.regione.pagamenti.ente.pagamentitelematicidovutipagati.PagamentiTelematiciDovutiPagati;
import it.veneto.regione.pagamenti.ente.pagamentitelematicidovutipagati.PagamentiTelematiciDovutiPagatiService;
import it.veneto.regione.pagamenti.ente.ppthead.IntestazionePPT;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiSingoloPagamentoPagati;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiSingoloVersamentoDovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDatiVersamentoDovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtDovuti;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtIdentificativoUnivocoPersonaFG;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtPagati;
import it.veneto.regione.schemas._2012.pagamenti.ente.CtSoggettoPagatore;
import it.veneto.regione.schemas._2012.pagamenti.ente.StTipoIdentificativoUnivocoPersFG;

@Service
public class EngineeringServiceImpl implements EngineeringService {
	
	@Autowired
	private PagamentoRepository pagamentoRepository;
	
	@Autowired
	private EventoRepository eventoRepository;
	
	@Autowired
	private PagDovutiLogRepository invioDovutiRepository;
	
	@Autowired
	private PagPagatiLogRepository chiediPagatiRepository;
	
	/** Non credo varierà, ma meglio parametrizzare e settare su file o tabella di configurazione */
	public static final String VERSIONE = "6.0";
	
	/** Parametri per connettersi al servizio. Da parametrizzare in quanto saranno diversi tra test e produzione */
	public static final String IPA = "C_D530";
	public static final String PASSWORD = "password";
	
	/** Id servizio assegnato da MyPay- DA parametrizzare in quanto dovrà essere aggiornato */
	public static final String SERVIZIO = "005";
	
	/** Permette tutti i tipi di pagamento. Si può modificare se necessario impedire certe forme di pagamento (vedi documentazione) */
	public static final String TIPO_VERSAMENTO_ALL = "ALL";
	
	public static final String ENDPOINT_PAGAMENTI = "http://payweb.ve.eng.it/pa/services/PagamentiTelematiciDovutiPagati"; // TEST ENGINEERING
	/** L'endpoint andrebbe parametrizzato, ovviamente sarà diverso tra test e produzione  */
	//public static final String ENDPOINT_PAGAMENTI = "https://paygov.collaudo.regione.veneto.it/pa/services/PagamentiTelematiciDovutiPagati"; // COLLAUDO REGIONE
	
	/** Da aggiornare con il dato definitivo (da concordare con Regione) */
	public static final String DATI_SPECIFICI_RISCOSSIONE = "9/123456"; //TODO Specifico in base a cosa pago, da concordare con RVE e Mola.
	
	/** Da aggiornare con il dato definitivo (da concordare con Regione) */
	public static final String TIPO_DOVUTI = "ALTRO";

	private static final boolean USE_PROXY = false;
	private static final String PROXY_HOST = ""; // DA VALORIZZARE CON PROXY
	private static final String PROXY_PORT = ""; // DA VALORIZZARE CON PORTA PROXY

	private static final String PROXY_USERNAME = ""; // PER ALCUNI PROXY POTREBBE ANCHE NON ESSERE VALORIZZATO. IN EFFETTO DOVREBBE ESSERE COSI' PER QUELLO DI REGIONE
	private static final String PROXY_PASSWORD = ""; // PASSWORD
	
	// costanti
	public static final String ENTE_NON_VALIDO = "PAA_ENTE_NON_VALIDO"; // codice IPA Ente non valido o password errata
	public static final String ID_SESSION_NON_VALIDO = "PAA_ID_SESSION_NON_VALIDO"; // idSession non valido
	public static final String PAGAMENTO_NON_INIZIATO = "PAA_PAGAMENTO_NON_INIZIATO"; // pagamento non iniziato
	public static final String PAGAMENTO_IN_CORSO = "PAA_PAGAMENTO_IN_CORSO"; // pagamento in corso
	public static final String PAGAMENTO_ANNULLATO = "PAA_PAGAMENTO_ANNULLATO"; // pagamento annullato
	public static final String PAGAMENTO_SCADUTO = "PAA_PAGAMENTO_SCADUTO"; // pagamento scaduto
	
	public static final String PAGAMENTO_ESEGUITO = "0";
	public static final String PAGAMENTO_NON_ESEGUITO = "1";
	public static final String PAGAMENTO_PARZIALMENTE_ESEGUITO = "2";
	public static final String DECORRENZA_TERMINI = "3";
	public static final String DECORRENZA_TERMINI_PARZIALE = "4";
	
	public static final Logger LOGGER = Logger.getLogger(EngineeringServiceImpl.class);
	
	private static JAXBContext jCtDovutiContext = null;
	private static JAXBContext jCtPagatiContext = null;
	private static JAXBContext jPaaSILInviaDovutiContext = null;
	
	
	
	protected static ThreadLocal<DateFormat> fmt = new ThreadLocal<DateFormat>() {
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyyMMdd'T'HHmmssSSS'Z'");
		}
	};
	
	protected static ThreadLocal<Transformer> tf = new ThreadLocal<Transformer>() {
		
		protected Transformer initialValue() {
			// An implementation of the TransformerFactory class is NOT guaranteed to be thread safe
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = null;
			try {
				transformer = tf.newTransformer();
			} catch (TransformerConfigurationException e) {
				LOGGER.error("Errore durante l'instanziazione del Transformer", e);
			}
			return transformer;
		}
	};
	
	protected static ThreadLocal<PagamentiTelematiciDovutiPagati> port = new ThreadLocal<PagamentiTelematiciDovutiPagati>() {
		
		protected PagamentiTelematiciDovutiPagati initialValue() {
		
			PagamentiTelematiciDovutiPagatiService service = new PagamentiTelematiciDovutiPagatiService();
			PagamentiTelematiciDovutiPagati port = service.getPagamentiTelematiciDovutiPagatiPort();
			
			BindingProvider bp = (BindingProvider) port;
			Map<String, Object> context = bp.getRequestContext();
			
			context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, ENDPOINT_PAGAMENTI);
			
			return port;
		}
	};
	
	public static synchronized JAXBContext getCtDovutiContext() throws JAXBException {
		 if (jCtDovutiContext == null) {
			 jCtDovutiContext = JAXBContext.newInstance(CtDovuti.class);
	     }
	     return jCtDovutiContext;
	}
	
	public static synchronized JAXBContext getCtPagatiContext() throws JAXBException {
		 if (jCtPagatiContext == null) {
			 jCtPagatiContext = JAXBContext.newInstance(CtPagati.class);
	     }
	     return jCtPagatiContext;
	}
	
	public static synchronized JAXBContext getPaaSILInviaDovutiContext() throws JAXBException {
		 if (jPaaSILInviaDovutiContext == null) {
			 jPaaSILInviaDovutiContext = JAXBContext.newInstance(PaaSILInviaDovuti.class);
	     }
	     return jPaaSILInviaDovutiContext;
	}
	
	/**
	 * Inizializzo qui il proxy, probabilmente va spostato in un punto centrale per l'applicativo.
	 */
	public EngineeringServiceImpl() {
		
		if (USE_PROXY) {
			System.setProperty("https.proxyHost", PROXY_HOST);
			System.setProperty("https.proxyPort", PROXY_PORT);
			if (StringUtils.isNotBlank(PROXY_USERNAME)) {
				Authenticator.setDefault(new HttpAuthenticateProxy(PROXY_USERNAME, PROXY_PASSWORD));
			}
		}
	}
	

	public String paga(Long idEvento, String backURL) throws Exception {

		Evento e = eventoRepository.findOne(idEvento);
		
		Pagamento p = pagamentoRepository.getPagamentoByEvento(e);
		if (p == null) {
			p = new Pagamento();
			p.setEvento(e);
		}
		
		Provider soggetto = e.getProvider();
		
		// i provider sono Ragioni Sociali, valorizzo i dati obbligatori.
		p.setAnagrafica(soggetto.getDenominazioneLegale());
		p.setCodiceFiscale(soggetto.getCodiceFiscale());
		p.setPartitaIva(soggetto.getPartitaIva());
		p.setEmail(soggetto.getAccount().getEmail());
		p.setTipoVersamento(EngineeringServiceImpl.TIPO_VERSAMENTO_ALL);
		p.setCausale("VERSAMENTO DI PROVA");
		p.setDatiSpecificiRiscossione(DATI_SPECIFICI_RISCOSSIONE); 
		
		// TODO E' necessario concordare un pattern per gli identificativi con 3D e RVE.
		String iud = StringUtils.rightPad(SERVIZIO + fmt.get().format(new Date()), 35, "0");
		LOGGER.info("IUD: " + iud);
		
		p.setIdentificativoUnivocoDovuto(iud);
		p.setImporto(e.getCosto());
		
		p.setDataInvio(new Date());
		pagamentoRepository.save(p);
		
		PaaSILInviaDovuti dovuti = createPagamentoMessage(p, backURL);
		
		IntestazionePPT header = new IntestazionePPT();
		header.setCodIpaEnte(IPA);
		
		PaaSILInviaDovutiRisposta response = port.get().paaSILInviaDovuti(dovuti, header);
		
		p.setIdSession(response.getIdSession());		
		e = p.getEvento();
		e.setPagInCorso(true);
		
		PagDovutiLog log = new PagDovutiLog();
		
		log.setPagamento(p);
		log.setDataRichiesta(new Date());
		log.setEsito(response.getEsito());
		log.setIdSession(response.getIdSession());
		if (response.getFault() != null) {
			log.setFaultCode(response.getFault().getFaultCode());
			log.setFaultString(response.getFault().getFaultString());
			log.setFaultDescription(response.getFault().getDescription());
			e.setPagInCorso(false);
		}

		pagamentoRepository.save(p);
		eventoRepository.save(e);
		invioDovutiRepository.save(log);
		
		return response.getUrl();
	}
	
	/**
	 * Crea l'oggetto PaaSILInviaDovuti per effettuare il pagamento su MyPay
	 * @param p l'oggetto Pagamento con le informazioni sul pagamento da effettuare
	 * @param backUrl l'url verso cui redirigere al termine o all'annullamento del pagamento.
	 * @return l'input per la chiamata al WS dei pagamenti
	 * @throws JAXBException 
	 * @throws TransformerException 
	 * @throws IOException 
	 */
	private PaaSILInviaDovuti createPagamentoMessage(Pagamento p, String backUrl) throws TransformerException, JAXBException, IOException {
		CtDatiSingoloVersamentoDovuti versamento = new CtDatiSingoloVersamentoDovuti();
		versamento.setCausaleVersamento(p.getCausale());
		versamento.setCommissioneCaricoPA(p.getCommissioneCaricoPa() != null ? BigDecimal.valueOf(p.getCommissioneCaricoPa()) : null);
		versamento.setDatiSpecificiRiscossione(p.getDatiSpecificiRiscossione());
		versamento.setIdentificativoTipoDovuto(TIPO_DOVUTI);
		versamento.setIdentificativoUnivocoDovuto(p.getIdentificativoUnivocoDovuto());
		versamento.setImportoSingoloVersamento(BigDecimal.valueOf(p.getImporto()));
		
		CtDatiVersamentoDovuti datiVersamento = new CtDatiVersamentoDovuti();
		datiVersamento.setTipoVersamento(p.getTipoVersamento());
		datiVersamento.getDatiSingoloVersamento().add(versamento);
		
		CtIdentificativoUnivocoPersonaFG u = new CtIdentificativoUnivocoPersonaFG();
		u.setCodiceIdentificativoUnivoco(StringUtils.isNotBlank(p.getCodiceFiscale()) ? p.getCodiceFiscale() : p.getPartitaIva());
		u.setTipoIdentificativoUnivoco(StTipoIdentificativoUnivocoPersFG.G); // soggetto giuridico
		
		CtSoggettoPagatore soggettoPagatore = new CtSoggettoPagatore();
		soggettoPagatore.setAnagraficaPagatore(p.getAnagrafica());
		soggettoPagatore.setCapPagatore(p.getCap());
		soggettoPagatore.setCivicoPagatore(p.getCivico());
		soggettoPagatore.setEMailPagatore(p.getEmail());
		soggettoPagatore.setIdentificativoUnivocoPagatore(u); // SICURO?
		soggettoPagatore.setIndirizzoPagatore(p.getIndirizzo());
		soggettoPagatore.setLocalitaPagatore(p.getLocalita());
		soggettoPagatore.setNazionePagatore(p.getNazione());
		soggettoPagatore.setProvinciaPagatore(p.getProvincia());
		
		CtDovuti ctDovuti = new CtDovuti();
		ctDovuti.setDatiVersamento(datiVersamento);
		ctDovuti.setSoggettoPagatore(soggettoPagatore);
		ctDovuti.setVersioneOggetto(VERSIONE); 
		
		
		PaaSILInviaDovuti dovuti = new PaaSILInviaDovuti();
		dovuti.setPassword(PASSWORD);
		
		// uso la trasformazione per omettere la dichiarazione XML
		Transformer transformer = tf.get();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");

		ByteArrayOutputStream w = new ByteArrayOutputStream();
		transformer.transform(new JAXBSource(getCtDovutiContext(), ctDovuti), new StreamResult(w));
		
		dovuti.setDovuti(w.toByteArray());
		w.close();
		
		dovuti.setEnteSILInviaRispostaPagamentoUrl(backUrl);
		
		// marshalling in byte[]
		final Marshaller m2 = getPaaSILInviaDovutiContext().createMarshaller();
		m2.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		m2.setProperty(Marshaller.JAXB_FRAGMENT, true);
		final StringWriter w2 = new StringWriter();
		m2.marshal(dovuti, w2);
		
		return dovuti;
	}
	
	public void esitoPagamenti() throws Exception {
		
		Set<Pagamento> pagamenti = pagamentoRepository.getPagamentiDaVerificare();
		
		Holder<FaultBean> fault;
		Holder<DataHandler> pagati;
		
		PagPagatiLog log;
		
		for (Pagamento p : pagamenti) {
			if (StringUtils.isNotBlank(p.getIdSession())) {
				fault = new Holder<FaultBean>();
				pagati = new Holder<DataHandler>();
				
				port.get().paaSILChiediPagati(IPA, PASSWORD, p.getIdSession(), fault, pagati);
				
				// traccio la chiamata
				log = new PagPagatiLog();
				log.setPagamento(p);
				log.setDataRichiesta(new Date());
				log.setIdSession(p.getIdSession());
				
				// In presenza di un fault code posso avere 2 casi: pagamento in corso (ancora senza esito), oppure fallito (es timeout).
				if (fault != null && fault.value != null) {
					log.setFaultCode(fault.value.getFaultCode());
					log.setFaultDescription(fault.value.getDescription());
					log.setFaultString(fault.value.getFaultString());
					
					// Se fault e diverso da non iniziato o in corso, il pagamento è fallito e va ripetuto.
					if (!PAGAMENTO_IN_CORSO.equals(log.getFaultCode()) && !PAGAMENTO_NON_INIZIATO.equals(log.getFaultCode())) {
						Evento e = p.getEvento();
						e.setPagInCorso(false);
						eventoRepository.save(e);
					}
					
				} else if (pagati != null && pagati.value != null) {
					String xml = new String(java.util.Base64.getDecoder().decode(IOUtils.toByteArray(pagati.value.getInputStream())));
					StringReader reader = new StringReader(xml);
					final Unmarshaller um = getCtPagatiContext().createUnmarshaller();
					CtPagati pagatiXml = (CtPagati)um.unmarshal(reader);
					
					Evento e = p.getEvento();
					// se sono qui il pagamento e' concluso: potrebbe essere andato a buon fine o meno.
					e.setPagInCorso(false);
					log.setCodiceEsito(pagatiXml.getDatiPagamento().getCodiceEsitoPagamento());
					p.setCodiceEsito(log.getCodiceEsito());
					
					// Se esito = 0 allora il pagamento risulta eseguito correttamente. Altrimenti deve essere rifatto.
					if (PAGAMENTO_ESEGUITO.equals(p.getCodiceEsito())) {
						e.setPagato(true);
					} else {
						e.setPagato(false);
					}
					eventoRepository.save(e);
					
					CtDatiSingoloPagamentoPagati item = pagatiXml.getDatiPagamento().getDatiSingoloPagamento().get(0);
					p.setDataEsitoSingoloPagamento(item.getDataEsitoSingoloPagamento().getTime());
					p.setEsitoSingoloPagamento(item.getEsitoSingoloPagamento());
					p.setIdentificativoUnivocoRiscosse(item.getIdentificativoUnivocoRiscossione());
					p.setImportoTotalePagato(item.getSingoloImportoPagato().doubleValue());
					
					// informazioni ridondanti in caso qualcuno dovesse ripetere il pagamento posso risalire allo storico.
					log.setDataEsitoSingoloPagamento(item.getDataEsitoSingoloPagamento().getTime());
					log.setEsitoSingoloPagamento(item.getEsitoSingoloPagamento());
					log.setIdentificativoUnivocoRiscosse(item.getIdentificativoUnivocoRiscossione());
					log.setImportoTotalePagato(item.getSingoloImportoPagato().doubleValue());
					
					pagamentoRepository.save(p);
				    
				}
				
				chiediPagatiRepository.save(log);
				
			}
		}
		
	}
	
	public void azzeraPagamenti(Long idProvider) throws Exception {
		Iterable<PagDovutiLog> pdl = invioDovutiRepository.findAll();
		invioDovutiRepository.delete(pdl);
		
		
		Iterable<PagPagatiLog> ppl = chiediPagatiRepository.findAll();
		chiediPagatiRepository.delete(ppl);
		
		Iterable<Pagamento> p = pagamentoRepository.findAll();
		pagamentoRepository.delete(p);
		
		Set<Evento> evs = eventoRepository.findAllByProviderId(idProvider);
		for (Evento ev : evs) {
			ev.setPagato(false);
			ev.setPagInCorso(false);
			eventoRepository.save(ev);
		}
	}
	
	public void firmaFile(Long idFile) {
		
	}

}