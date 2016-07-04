package it.tredi.ecm.service;

import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.tredi.ecm.dao.entity.Anagrafica;
import it.tredi.ecm.dao.repository.AnagraficaRepository;

@Service
public class AnagraficaServiceImpl implements AnagraficaService {
	private static Logger LOGGER = LoggerFactory.getLogger(AnagraficaServiceImpl.class);
	
	@Autowired
	private AnagraficaRepository anagraficaRepository;
	
	@Override
	public Set<Anagrafica> getAllAnagraficheByProviderId(Long providerId) {
		LOGGER.debug("Recupero tutte le anagrafiche del provider " + providerId);
		return anagraficaRepository.findAllByProviderId(providerId);
	}

	@Override
	public Anagrafica getAnagrafica(Long id) {
		LOGGER.debug("Recupero anagrafica " + id);
		return anagraficaRepository.findOne(id);
	}
	
	@Override
	public Optional<Long> getAnagraficaIdWithCodiceFiscaleForProvider(String codiceFiscale, Long providerId) {
		LOGGER.debug("Controllo univocità dell'anagrafica con codice fiscale " + codiceFiscale + " per il provider " + providerId);
		return anagraficaRepository.findOneByCodiceFiscaleAndProviderId(codiceFiscale, providerId);
	}
}
