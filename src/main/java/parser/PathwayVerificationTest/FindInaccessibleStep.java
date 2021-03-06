package parser.PathwayVerificationTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.variables.BoolVar;
import org.eclipse.emf.common.util.EList;

import pathwayMetamodel.Element;
import pathwayMetamodel.Pathway;
import pathwayMetamodel.Sequence;

public class FindInaccessibleStep extends SequenceParser {
	
	public FindInaccessibleStep(Pathway pathway) {
		super(pathway);
	}

	//Return a set of Elements that are inaccessible steps.
	//An element is a inaccessible step if there are no valid path to get to the element.
	public Map<String, Double> findInaccessibleSteps() {
		Map<String, Double> mapStatistics = new HashMap<String, Double>();
		int binaryTree = 0;
		List<Element> accessibleElements = new ArrayList<Element>();
		List<Element> visitedElements = new ArrayList<Element>();
		Stack<Element> elementsStack = new Stack<Element>();
		List<Sequence> sequenceList = new ArrayList<Sequence>();
		//Get the initial step (root) of the pathway and put it in the stack, the lists of 
		//visited elements  and accessible elements (The initial step is always accessible)
		Element initialElement = getInitialStep(pathway.getElement());
		accessibleElements.add(initialElement);
		visitedElements.add(initialElement);
		elementsStack.push(initialElement);
		//Loop to find all accessible elements
		//It works as depth-first search
		while(elementsStack.isEmpty() == false) {
			Model model = new Model("Find All Solutions: " + 1); //Create a model to verify a valid path with ChocoSolver.
			Element element = elementsStack.lastElement(); //get the last element from the stack
			Sequence outputSequence = getNextSequence(element, visitedElements);//get the next sequence to be verified
			//Check if there are no more step to be verified from element
			//Remove the element from the stack and the last sequence from the sequenceList
			if(outputSequence == null) {
				elementsStack.pop();
				if(sequenceList.size() > 0) {
					sequenceList.remove(sequenceList.size()-1);
				}
			}else {
				//Get the output sequece from element an add to sequenceList
				sequenceList.add(outputSequence);	
				List<BoolVar> boolSequences = sequenceListToBoolVarList(model, sequenceList);//Get a list of BoolVar from a list of sequence of the mapElementOutputSequences.
				if (boolSequences!=null && !boolSequences.isEmpty()) {
					//Creates the constraint that ensures that all operations on the list sequences must be true.
					for(int j = 0; j < boolSequences.size(); j++) {
						model.arithm(boolSequences.get(j), "=", 1).post();//Post the constraint "the sequence have to be true" to the model.
					}
					//Check if there is at least a solution with that constraints above
					//if truth means that the element is accessible
					//if false means that it does not matter to look at the next step, then remove the last sequence from the list.
					if(model.getSolver().findSolution() != null) {
						accessibleElements.add(outputSequence.getInputStep());
						//Checks for a next step to add the element to the stack. 
						//Otherwise, remove the last sequence from sequenceList
						if(outputSequence.getInputStep() != null && outputSequence.getInputStep().getOutputSequences() != null && outputSequence.getInputStep().getOutputSequences().size() != 0) {
							elementsStack.push(outputSequence.getInputStep());
						}else {
							sequenceList.remove(sequenceList.size()-1);
						}
					}else {
						sequenceList.remove(sequenceList.size()-1);
					}
					double nbVars = (double) model.getNbVars();
					mapStatistics.put("Variables", mapStatistics.containsKey("Variables")?mapStatistics.get("Variables")+nbVars:nbVars);
					double nbCstrs = (double) model.getNbCstrs();
					mapStatistics.put("Constraints", mapStatistics.containsKey("Constraints")?mapStatistics.get("Constraints")+nbCstrs:nbCstrs);
					double timeCount = (double) model.getSolver().getTimeCount();
					mapStatistics.put("ResolutionTime", mapStatistics.containsKey("ResolutionTime")?mapStatistics.get("ResolutionTime")+timeCount:timeCount);
					double nodeCount = (double) model.getSolver().getNodeCount();
					mapStatistics.put("Nodes", mapStatistics.containsKey("Nodes")?mapStatistics.get("Nodes")+nodeCount:nodeCount);
					double backTrackCount = (double) model.getSolver().getBackTrackCount();
					mapStatistics.put("Backtracks", mapStatistics.containsKey("Backtracks")?mapStatistics.get("Backtracks")+backTrackCount:backTrackCount);
					double failCount = (double) model.getSolver().getFailCount();
					mapStatistics.put("Fails", mapStatistics.containsKey("Fails")?mapStatistics.get("Fails")+failCount:failCount);
					double restartCount = (double) model.getSolver().getRestartCount();
					mapStatistics.put("Restarts", mapStatistics.containsKey("Restarts")?mapStatistics.get("Restarts")+restartCount:restartCount);
					double nbSolutions = (double) model.getSolver().getSolutionCount();
					mapStatistics.put("Solutions", mapStatistics.containsKey("Solutions")?mapStatistics.get("Solutions")+nbSolutions:nbSolutions);
					if(nodeCount > 0) {
						binaryTree++;
					}
				}
			}
		}
		//With the set of all accessible steps from the pathway, we can prone the set of all elements
		//and get just the inaccessible steps
		mapStatistics.put("BinaryTrees", (double) binaryTree);
		return mapStatistics;
	}

	//return the next sequence to be verified
	private Sequence getNextSequence(Element element, List<Element> visitedElements) {
		//Loop to find the next step and sequence to be verified
		for(int index = 0; index < element.getOutputSequences().size(); index++) {
			Sequence outputSequence = element.getOutputSequences().get(index);
			Element inputStep = outputSequence.getInputStep();
			//Check if the step has not already been visited
			if(!visitedElements.contains(inputStep)) {
				visitedElements.add(inputStep);
				return outputSequence;
			}
		}
		return null;
	}
	
	//Method to remove all accessible steps from the set of pathway elements.
	private List<Element> getInaccessibleElements(List<Element> elements, List<Element> accessibleElements) {
		List<Element> inaccessibleElements = new ArrayList<Element>();
		for(int i = 0; i < elements.size(); i++) {
			if(!accessibleElements.contains(elements.get(i))) {
				inaccessibleElements.add(elements.get(i));
			}
		}
		return inaccessibleElements;
	}
	
	//Method to get the initial step from the pathway
	private Element getInitialStep(List<Element> elementList) {
		for (int i = 0; i < elementList.size(); i++) {
			if(elementList.get(i).isIsInitial()) {
				return elementList.get(i);
			}
//			if(elementList.get(i).getInputSequences().size() == 0 && elementList.get(i).getOutputSequences().size() > 0) {
//				return elementList.get(i);
//			}
		}
		return null;
	}
}
