package mcastro.nlp.ontology;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonValue;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.FileManager;

/**
 * SESkillsMatcher is intended to match skill in a job offer against skills in a resume with with help of a Software Engineering Ontology 
 * @author mcastro
 *
 */
public class SESkillsMatcher {

	/**  SE ontology file name */
    public static final String ONTOLOGY_PATH = "software-engineering.ontology.owl";
    /** SE ontology namespace */
    public static final String SE_NS = "http://www.semanticweb.org/mcastro/ontologies/2018/5/software-engineering-ontology#";
    /** JSON Key for offersAndResumes element */    
    public static final String OFFERS_AND_RESUMES_JSON_KEY = "offersAndResumes";
    /** JSON Key for offerSkills element */
    public static final String OFFER_SKILLS_JSON_KEY = "offerSkills";
    /** JSON Key for resumeSkills element */
    public static final String RESUMES_SKILLS_JSON_KEY = "resumeSkills";
    
    /**
     * Finds an ontology class if found by the given class name (and the namespace)
     * @param model ontology model
     * @param className class name to look for
     * @return an ontology class or <code>null</code>
     */
    public OntClass getClass(OntModel model, String className) {
    	return model.getOntClass(SE_NS + className);
    }
    
    /**
     * Finds ontology super classes for the given class name
     * @param model ontology model
     * @param className class name to look for
     * @return a list of ontology super classes
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
     * Finds ontology subclasses for the given class name
     * @param model ontology model
     * @param className class name to look for
     * @return a list of ontology subclasses
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
     * Converts a list of ontology classes to a list of strings with the names of those classes
     * @param ontClassList a list of ontology classes
     * @return a list of strings with the names of those classes
     */
    public List<String> getOntClassListAsStringList(List<OntClass> ontClassList) {
    	List<String> classNames = new ArrayList<>();
    	
		for (OntClass ontSuperClass : ontClassList) {
			classNames.add(ontSuperClass.getLocalName().toLowerCase());
		}
		
		return classNames;
    }
    
    /**
     * Converts a JSON array to a list of strings with the values in the array
     * @param jsonArray JSON array
     * @return list of strings with the values in the array
     */
    public List<String> getListFromJsonArray(JsonArray jsonArray) {
    	List<String> list = new ArrayList<>();
    	
    	for (ListIterator<JsonValue> it = jsonArray.listIterator(); it.hasNext(); ) {
    		// remove quotation marks and convert to lower case
    		list.add(it.next().toString().replace("\"", "").toLowerCase());    		
    	}
    	
    	return list;
    }
    
    /**
     * Calculates a score per each skill required in the offer, and places scores in an array that is as big as the number of required skills
     * @param offerSkills skills required in the offer
     * @param resumeSkills skills present in the resume
     * @return an array with scores that is as big as the number of required skills
     */
    public List<Float> getMatchingScore(JsonValue offerSkills, JsonValue resumeSkills) {    	
    	List<String> offerSkillsArray = new ArrayList<>();
    	List<String> resumeSkillsArray = new ArrayList<>();
    	List<Float> matchingArray = new ArrayList<>();
    	
		// create ontology model
    	OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
		FileManager.get().readModel(model, ONTOLOGY_PATH);
    	
    	// get list of offer skills 
		offerSkillsArray = getListFromJsonArray(offerSkills.getAsArray());
    	System.out.println("Offer Skills: " + offerSkillsArray);
    	
    	// get list of resume skills
    	resumeSkillsArray = getListFromJsonArray(resumeSkills.getAsArray());
    	System.out.println("Resume Skills: " + resumeSkillsArray);
    	
    	// start processing based on what the offer requires
    	for (String offerSkill: offerSkillsArray) { 		
    		if (resumeSkillsArray.contains(offerSkill)) {
    			// resume contains the skill, add 1
    			matchingArray.add(1f);
    		}
    		else {
    			// calculate a secondary score for a skill in the offer by looking at super classes and subclasses of that skill, 
    			// and if any of those are present in the resume skills list
    			System.out.println(offerSkill + " NOT Found in Resume");
    			matchingArray.add(calculateSecondaryScore(model, offerSkill, resumeSkillsArray));
			}
		}
    	
    	return matchingArray;
    }

	/**
	 * Calculates a secondary score for a skill in the offer by looking at super classes and subclasses of that skill and if 
	 * any of those are present in the resume skills list
	 * @param model ontology model
	 * @param offerSkill offer skill required
	 * @param resumeSkillsArray all skills available in the resume
	 * @return a secondary score for a skill
	 */
	private float calculateSecondaryScore(OntModel model, String offerSkill, List<String> resumeSkillsArray) {
		List<String> superClasses = getOntClassListAsStringList(getSuperClasses(model, offerSkill));
		List<String> subClasses = getOntClassListAsStringList(getSubClasses(model, offerSkill));
		float secondaryScore = 0f;

		System.out.println("\tSuperClasses: " + superClasses);
		System.out.println("\tSubClasses: " + subClasses);
		
		// if the resume contains a skill present as super class of the required skill in the offer, add 0.5
		for (String relatedSkill : superClasses) {
			if (resumeSkillsArray.contains(relatedSkill)) {
				secondaryScore = 0.5f;
			}
		}
		
		// if the resume contains a skill present as sub class of the required skill in the offer, add 0.5
		for (String relatedSkill : subClasses) {
			if (resumeSkillsArray.contains(relatedSkill)) {
				secondaryScore += 0.5f;
			}
		}
		
		// possible values for secondaryScore should only be 0, 0.5 and 1 
		if (secondaryScore > 1) {
			secondaryScore = 1;
		}
		
		return secondaryScore;
	}
	
    /**
     * Takes a list of values with the score for each skill required in an offer, and returns a unified score 
     * @param matchingArray list of values with the score for each skill required in an offer
     * @return a unified score
     */
    public float calculateTotalScore(List<Float> matchingArray) {
    	int numberOfDesiredSkills = matchingArray.size();
    	float maxPercentagePerSkill =  1f / numberOfDesiredSkills;
    	float totalScore = 0f;
    	
    	for (Float skillScore : matchingArray) {
    		// each skill's score is multiplied by maxPercentagePerSkill so we can have 1 as max possible score 
    		totalScore += (skillScore * maxPercentagePerSkill);
    	}
    	
    	return totalScore;
    }
    
    /**
	 * Main method. Reads JSON file with offers and resumes and starts the matching and scoring process
	 * @param args expects a JSON file name as first and only argument 
	 */
    public static void main(String[] args) {
		SESkillsMatcher seSkillsMatcher = new SESkillsMatcher();
		
		if (args.length > 0) {
			try {
				JsonObject offersAndResumes = JSON.parse(new FileInputStream(new File(args[0])));
				
				for (JsonValue offerAndResume : offersAndResumes.get(OFFERS_AND_RESUMES_JSON_KEY).getAsArray()) {
					JsonValue offerSkills = offerAndResume.getAsObject().get(OFFER_SKILLS_JSON_KEY).getAsArray();
					JsonValue resumeSkills = offerAndResume.getAsObject().get(RESUMES_SKILLS_JSON_KEY).getAsArray();

					List<Float> matchingArray = seSkillsMatcher.getMatchingScore(offerSkills, resumeSkills);
					float calculatedScore = seSkillsMatcher.calculateTotalScore(matchingArray);
					
			    	System.out.println("Matching per Skill: " + matchingArray);
			    	System.out.println("Calculated Total Score: " + calculatedScore);
					System.out.println("\n");
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		else {
			System.out.println("Please provide the JSON file name with offers and resumes information as parameter.");
		}
	}

}
