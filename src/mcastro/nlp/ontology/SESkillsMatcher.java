package mcastro.nlp.ontology;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonValue;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.FileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SESkillsMatcher {

    @SuppressWarnings( value = "unused" )
    private static final Logger log = LoggerFactory.getLogger(SESkillsMatcher.class);
    // SE ontology namespace
    public static final String SE_NS = "http://www.semanticweb.org/mcastro/ontologies/2018/5/software-engineering-ontology#";
    
    public static final String JSON_SKILLS_KEY = "skills"; 
    
    /**
     * 
     * @param model
     * @param className
     * @return
     */
    public OntClass getClass(OntModel model, String className) {
    	return model.getOntClass(SE_NS + className);
    }
    
    /**
     * 
     * @param model
     * @param className
     * @return
     */
    public List<OntClass> getSuperClasses(OntModel model, String className) {
    	OntClass ontClass = getClass(model, className);
    	List<OntClass> superClasses= new ArrayList<>();
    	
    	if (ontClass != null) {
    		superClasses = ontClass.listSuperClasses().toList();
    	}
    	
    	return superClasses;
    }
    
    /**
     * 
     * @param model
     * @param className
     * @return
     */
    public List<OntClass> getSubClasses(OntModel model, String className) {
    	OntClass ontClass = getClass(model, className);
    	List<OntClass> superClasses= new ArrayList<>();
    	
    	if (ontClass != null) {
    		superClasses = ontClass.listSubClasses().toList();
    	}
    	
    	return superClasses;
    }
    
    /**
     * 
     * @param ontClassList
     * @return
     */
    public List<String> getOntClassListAsStringList(List<OntClass> ontClassList) {
    	List<String> classNames = new ArrayList<>();
    	
		for (OntClass ontSuperClass : ontClassList) {
			classNames.add(ontSuperClass.getLocalName().toLowerCase());
		}
		
		return classNames;
    }
    
    /**
     * 
     * @param jsonArray
     * @return
     */
    public List<String> getListFromJsonArray(JsonArray jsonArray) {
    	List<String> list = new ArrayList<>();
    	
    	for (ListIterator<JsonValue> it = jsonArray.listIterator(); it.hasNext(); ) {
    		list.add(it.next().toString().replace("\"", "").toLowerCase());    		
    	}
    	
    	return list;
    }
    
    /**
     * 
     * @param matchingArray
     * @return
     */
    public float getCalculatedTotalScore(List<Float> matchingArray) {
    	int numberOfDesiredSkills = matchingArray.size();
    	float maxPercentagePerSkill =  1f / numberOfDesiredSkills;
    	float totalScore = 0f;
    	
    	for (Float skillScore : matchingArray) {
    		totalScore += (skillScore * maxPercentagePerSkill);
    	}
    	
    	return totalScore;
    }
    
    /**
     * 
     * @param offerSkills
     * @param resumeSkills
     */
    public void getMatchingScore(JsonValue offerSkills, JsonValue resumeSkills) {    	
    	List<String> offerSkillsArray = new ArrayList<>();
    	List<String> resumeSkillsArray = new ArrayList<>();
    	List<Float> matchingArray = new ArrayList<>();
    	
		String ontologyPath = "software-engineering.ontology.owl";
		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		FileManager.get().readModel(model, ontologyPath);
		//model.write(System.out) ; // print the owl file to make sure that you did
    	
    	offerSkillsArray = getListFromJsonArray(offerSkills.getAsArray());
    	System.out.println("Offer Skills: " + offerSkillsArray);
    	
    	resumeSkillsArray = getListFromJsonArray(resumeSkills.getAsArray());
    	System.out.println("Resume Skills: " + resumeSkillsArray);
    	
    	for (String offerSkill: offerSkillsArray) {
    		List<String> superClasses = null;
    		List<String> subClasses = null;
    		
    		if (resumeSkillsArray.contains(offerSkill)) {
    			matchingArray.add(1f);
    		}
    		else {
    			float secondaryScore = 0f;
    			superClasses = getOntClassListAsStringList(getSuperClasses(model, offerSkill));
    			subClasses = getOntClassListAsStringList(getSubClasses(model, offerSkill));
    			System.out.println(offerSkill + " NOT Found in Resume");
    			System.out.println("\tSuperClasses: " + superClasses);
    			System.out.println("\tSubClasses: " + subClasses);
    			
    			for (String relatedSkill : superClasses) {
    				if (resumeSkillsArray.contains(relatedSkill)) {
    					secondaryScore = 0.5f;
    				}
    			}
    			
    			for (String relatedSkill : subClasses) {
    				if (resumeSkillsArray.contains(relatedSkill)) {
    					secondaryScore = 0.5f;
    				}
    			}
    			matchingArray.add(secondaryScore);
    		}
    	}
    	System.out.println("Matching per Skill: " + matchingArray);
    	System.out.println("Calculated Total Score: " + getCalculatedTotalScore(matchingArray));
    }
	
	/**
	 * 
	 * @param args
	 */
    public static void main(String[] args) {
		SESkillsMatcher seSkillsMatcher = new SESkillsMatcher();
		
		if (args.length > 0) {
			try {
				JsonObject offersAndResumes = JSON.parse(new FileInputStream(new File(args[0])));
				
				for (JsonValue offerAndResume : offersAndResumes.get("offersAndResumes").getAsArray()) {
					JsonValue offerSkills = offerAndResume.getAsObject().get("offerSkills").getAsArray();
					JsonValue resumeSkills = offerAndResume.getAsObject().get("resumeSkills").getAsArray();

					seSkillsMatcher.getMatchingScore(offerSkills, resumeSkills);
					System.out.println("\n");
				}
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
