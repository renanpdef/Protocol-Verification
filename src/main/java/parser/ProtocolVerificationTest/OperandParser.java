package parser.ProtocolVerificationTest;

import java.util.List;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import protocolosv2.Operand;
import protocolosv2.Operation;
import protocolosv2.Operator;

public class OperandParser {
	int index = 0;
	
	//get the operands from operation and put it in boolVars list or intVars list.
	//Create the variable for model.
	public void operandsIntoLists(List<BoolVar> boolVars, List<IntVar> intVars, Model model, Operation operation) {
		//Go through all operands in operation.
		for(int i = 0; i < operation.getOperand().size(); i++) {
			Model auxModel = new Model("Auxiliary Model");
			//If operand is a operation.
			if(operation.getOperand().get(i).getClass().toString().contains("Operation")) {
				operandsIntoLists(boolVars, intVars, model, (Operation) operation.getOperand().get(i));
			}
			//if operand is a Numeric operand.
			else if(operation.getOperand().get(i).getClass().toString().contains("Numeric")) {
				//if the name of operand is null or "".
				if(operation.getOperand().get(i).getName() == null || operation.getOperand().get(i).getName() == "") {
					String name = operation.getOperator().getName() + index++;		
					operation.getOperand().get(i).setName(name);
				}
				IntVar intVar = auxModel.intVar(operation.getOperand().get(i).getName(), new int[] {0,1,2,3,4,25,50,75,100});
				//if intVars list still doesn't contain the new intVar.
				if(!containsIntVar(intVars, intVar)) {
					double operandValue = getOperandValue(operation.getOperand().get(i).toString());
					if(operandValue != 0) {
						intVars.add(model.intVar(operation.getOperand().get(i).getName(), (int)operandValue));
					}else {
						intVars.add(model.intVar(operation.getOperand().get(i).getName(), new int[] {0,1,2,3,4,25,50,75,100}));
					}
				}
				
			}
			//if operand is a YesOrNo (boolean) operand.
			else {
				//if the name of operand is null or "".
				if(operation.getOperand().get(i).getName() == null || operation.getOperand().get(i).getName() == "") {
					String name = operation.getOperator().getName() + index++;		
					operation.getOperand().get(i).setName(name);
				}
				
				if(operation.getOperator() == Operator.SUM || operation.getOperator() == Operator.MINUS || operation.getOperator() == Operator.MULTIPLICATION || operation.getOperator() == Operator.DIVISION) {
					IntVar intVar = auxModel.intVar(operation.getOperand().get(i).getName(), new int[] {0,1});
					//if intVars list still doesn't contain the new intVar.
					if(!containsIntVar(intVars, intVar)) {
						double operandWeight = getOperandWeight(operation.getOperand().get(i).toString());
						intVars.add(model.intVar(operation.getOperand().get(i).getName(), new int[] {0, (int)operandWeight}));
					}
				}else {
					BoolVar boolVar = auxModel.boolVar(operation.getOperand().get(i).getName());
					//if boolVars list don't already contain the new boolVar.
					if(!containsBoolVar(boolVars, boolVar)) {
						boolVars.add(model.boolVar(operation.getOperand().get(i).getName()));
					}
				}
			}	
		}
	}
		
	//Verify whether list boolVars already has the boolvar
	public boolean containsBoolVar(List<BoolVar> boolVars, BoolVar boolVar) {
		String name = boolVar.getName();
		for(int i = 0; i < boolVars.size(); i++) {
			String bName = boolVars.get(i).getName();
			if(bName.equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}
	
	//Verify whether list intVars already has the intVar
		public boolean containsIntVar(List<IntVar> intVars, IntVar intVar) {
			String name = intVar.getName();
			for(int i = 0; i < intVars.size(); i++) {
				String bName = intVars.get(i).getName();
				
				if(bName.equalsIgnoreCase(name)) {
					return true;
				}			
			}
			return false;
		}
	
	//retun the index of a operand from the list boolVars. 
	public int indexOfBoolVar(List<BoolVar> boolVars, Operand operand) {
		String name = operand.getName();
		for(int i = 0; i < boolVars.size(); i++) {
			String bName = boolVars.get(i).getName();
			if(bName.equalsIgnoreCase(name)) {
				return i;
			}
		}
		return -1;
	}
	
	//retun the index of a operand from the list intVars.
		public int indexOfIntVar(List<IntVar> intVars, Operand operand) {
			String name = operand.getName();
			for(int i = 0; i < intVars.size(); i++) {
				String bName = intVars.get(i).getName();
				if(bName.equalsIgnoreCase(name)) {
					return i;
				}
			}
			return -1;
		}
		
		private double getOperandValue(String str) {
			int pos1 = str.lastIndexOf("(")+1;
			int pos2 = str.lastIndexOf(")");
			char[] charValue = new char[pos2-pos1];
			str.getChars(pos1, pos2, charValue, 0);
			String strValue = String.copyValueOf(charValue);
			strValue = strValue.split(",")[2];
			strValue = strValue.toString().replaceFirst(" value: ", "");
			double doubleValue = 0;
			try {
				doubleValue = Double.parseDouble(strValue);
			}catch(NumberFormatException e){
			}
			return doubleValue;
		}
		
		private double getOperandWeight(String str) {
			int pos1 = str.lastIndexOf("(")+1;
			int pos2 = str.lastIndexOf(")");
			char[] charWeight = new char[pos2-pos1];
			str.getChars(pos1, pos2, charWeight, 0);
			String strWeight = String.copyValueOf(charWeight);
			strWeight = strWeight.split(",")[1];
			strWeight = strWeight.toString().replaceFirst(" weight: ", "");
			double doubleValue = 0;
			try {
				doubleValue = Double.parseDouble(strWeight);
			}catch(NumberFormatException e){
			}
			return doubleValue;
		}
}