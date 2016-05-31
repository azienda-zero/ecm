package it.tredi.ecm.web.bean;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.web.multipart.MultipartFile;
import it.tredi.ecm.dao.entity.File;
import it.tredi.ecm.dao.entity.Persona;
import it.tredi.ecm.dao.enumlist.Costanti;
import it.tredi.ecm.dao.enumlist.Ruolo;
import it.tredi.ecm.utils.MultiPartBuilder;
import it.tredi.ecm.utils.Utils;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonaWrapper extends Wrapper {
	private Persona persona;
	private Ruolo ruolo;
	private Long accreditamentoId;
	private Long providerId;
	
	private File attoNomina;
	private File cv;
	private File delega;
	
	public void setOffsetAndIds(){
		if(persona.isLegaleRappresentante())
			setIdOffset(22);
		else if(persona.isLegaleRappresentante())
			setIdOffset(31);
		else if(persona.isResponsabileSegreteria())
			setIdOffset(46);
		else if(persona.isResponsabileAmministrativo())
			setIdOffset(53);
		else if(persona.isResponsabileSistemaInformatico())
			setIdOffset(71);
		else if(persona.isResponsabileQualita())
			setIdOffset(79);
	}
	
	public PersonaWrapper(){
		setAttoNomina(new File());
		setCv(new File());
		setDelega(new File());
	}
	
	public void setAttoNomina(File file){
		if(file.getId() != null){
			//file è quello letto da DB
			attoNomina.setData(file.getData().clone());
		}else if(attoNomina != null){
			//file è quello ke sta arrivando da MultiPart
			file.setId(attoNomina.getId());
		}
		
		attoNomina = file;
		attoNomina.setNomeFile(file.getNomeFile());
		attoNomina.setTipo(Costanti.FILE_ATTO_NOMINA);
		attoNomina.setPersona(persona);
		attoNomina.setDataCreazione(LocalTime.now());
		attoNomina.setProvider(file.getProvider());
	}
	
	public void setCv(File file){
		cv = file;
		cv.setTipo(Costanti.FILE_CV);
		cv.setPersona(persona);
		cv.setDataCreazione(LocalTime.now());
	}
	
	public void setDelega(File file){
		delega = file;
		delega.setTipo(Costanti.FILE_DELEGA);
		delega.setPersona(persona);
		delega.setDataCreazione(LocalTime.now());
	}
	
	public Set<File> getFiles(){
		Set<File> files = new HashSet<File>();
		files.add(attoNomina);
		files.add(cv);
		files.add(delega);
		return files;
	}
	
	public void setAttoNomina_persona(MultipartFile multiPartFile){
		setAttoNomina(Utils.convertFromMultiPart(multiPartFile));
	}
	public void setCv_persona(MultipartFile multiPartFile){
		setCv(Utils.convertFromMultiPart(multiPartFile));
	}
	public void setDelega_persona(MultipartFile multiPartFile){
		setDelega(Utils.convertFromMultiPart(multiPartFile));
	}
}
