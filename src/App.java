import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class App extends Application {
	public static final boolean DEBUG = false;
	public static Comparator<HashMap<Character, Integer>> termOrder = (t1, t2) -> rateTermOrder(t1).compareTo(rateTermOrder(t2));
	
	public static VBox stepsBox = new VBox();
	public static LinkedList<String> steps = new LinkedList<>();
	
	public static void main(String[] args) {
		launch(args);
	}
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		try {
			VBox rootBox = new VBox();
			Label[] instructions = {
					new Label("Welcome to Anthony Gazzarato's Quine-McCluskey application!"),
					new Label("Variables should be single characters."),
					new Label("Use a preceding ~ to indicate complimentation."),
					new Label("Example expression: xyz + x~yz + x~y + x~y~z (should evaluate to \"x~y + xz\")"),
					new Label("Please input boolean expression to minimize below:")
			};
			
			HBox input = new HBox();
			TextField inputText = new TextField();
			inputText.setPromptText("Input your expression here.");
			inputText.setText("xyz + x~yz + x~y + x~y~z");
			Button inputButton = new Button("Evaluate");
			inputButton.setOnAction(e -> {
				quineMcCluskey(inputText.getText());
			});
			input.getChildren().addAll(inputText, inputButton);
			
			rootBox.getChildren().addAll(instructions);
			rootBox.getChildren().add(input);
			rootBox.getChildren().add(stepsBox);

			ScrollPane root = new ScrollPane(rootBox);
			Scene scene = new Scene(root, 400, 400);
			primaryStage.setScene(scene);
			primaryStage.setTitle("Quine-McCluskey");
			primaryStage.show();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static void quineMcCluskey(String input) {
		steps = new LinkedList<String>();
		stepsBox.getChildren().clear();
		
		String[] termStrings = input.split("\\+");
		
		LinkedList<HashMap<Character, Integer>> terms = new LinkedList<HashMap<Character, Integer>>();
		for(int i = 0; i < termStrings.length; i++) {
			String termString = termStrings[i].trim();
			binaryInsert(stringToTermMap(termString), terms, (t1,t2)->compareOnes(t1,t2));
		}
		terms = recursiveQM(terms);
		terms.sort(termOrder);
		
		String tS = "Final Expression: " + expressionToString(terms);
		steps.add(tS);
		for(String s : steps) {
			stepsBox.getChildren().add(new Label(s));
		}
	}
	
	private static LinkedList<HashMap<Character, Integer>> recursiveQM(LinkedList<HashMap<Character, Integer>> terms) {
		LinkedList<HashMap<Character, Integer>>
				combinedTerms = new LinkedList<>(),
				obsoleteTerms = new LinkedList<>(),
				oldTerms = new LinkedList<>();
		
		terms.forEach(t -> oldTerms.add(t));
		steps.add(expressionToString(terms));
		
		for(int i = 0; i < terms.size(); i++) {
			HashMap<Character, Integer> term1 = terms.get(i);
			int termOnes = numberOfOnes(term1);
			for(int j = i; j < terms.size(); j++) {
				HashMap<Character, Integer> term2 = terms.get(j);
				if(Math.abs(termOnes - numberOfOnes(term2)) == 1) {
					if(canCombine(term1, term2)) {
						HashMap<Character, Integer> combined = combine(term1, term2);
						steps.add(termToString(term1)+" + "+termToString(term2)+" = "+termToString(combined));
						combinedTerms.add(combined);
						obsoleteTerms.add(term1);
						obsoleteTerms.add(term2);
					}
				} else if(DEBUG) {
					System.out.println("Term 1 has " + termOnes + " ones; term 2 has " + numberOfOnes(term2));
				}
			}
		}
		combinedTerms.forEach((term) -> binaryInsert(term, terms, (t1,t2) -> compareOnes(t1,t2)));
		obsoleteTerms.forEach((term) -> { if(terms.contains(term)) terms.remove(term); });
		removeDuplicates(terms);
		
		if(oldTerms.equals(terms)) {
			steps.add("No further reductions found.");
			return terms;
		} else {
			steps.add("Minterm Expansion: " + expressionToString(terms));
			steps.add("");
			return recursiveQM(terms);
		}
	}
	
	private static <E> void binaryInsert(E elem, LinkedList<E> list, Comparator<E> comp) {
		if(list.isEmpty()) {
			list.add(elem);
			return;
		}
		
		binaryInsert(elem, list, comp, 0, list.size()-1);
	}
	private static <E> void binaryInsert(E elem, LinkedList<E> list, Comparator<E> comp, int l, int r) {
		if(l <= r) {
			int m = (l + r) / 2;
			int comparison = comp.compare(elem, list.get(m));
			if(comparison == 0) {
				list.add(m, elem);
				return;
			} else if(comparison < 0) {
				binaryInsert(elem, list, comp, l, m-1);
			} else {
				binaryInsert(elem, list, comp, m+1, r);
			}
		} else {
			list.add(l, elem);
		}
	}
	private static <E> void removeDuplicates(LinkedList<E> list) {
		HashSet<E> set = new HashSet<>(list);
		list.clear();
		for(E elem : set) {
			list.add(elem);
		}
	}
	
	private static boolean canCombine(HashMap<Character, Integer> term1, HashMap<Character, Integer> term2) {
		if(term1.values().equals(term2.values())) {
			if(DEBUG) { System.out.println("Terms and match; can't combine."); }
			return false;
		}
		if(term1.keySet().equals(term2.keySet())) {
			int difference = 0;
			for(Character key : term1.keySet()) {
				if(!term1.get(key).equals(term2.get(key))) {
					difference++;
				}
				if(difference > 1) {
					if(DEBUG) { System.out.println("More than one term is different."); }
					return false;
				}
			}
			return difference == 1;
		} else {
			if(DEBUG) { System.out.println("Keysets do not match."); }
			return false;
		}
	}
	private static HashMap<Character, Integer> combine(HashMap<Character, Integer> term1, HashMap<Character, Integer> term2) {
		HashMap<Character, Integer> combined = new HashMap<>();
		for(Character key : term1.keySet()) {
			if(term1.get(key).equals(term2.get(key))) {
				combined.put(key, term1.get(key));
			}
		}
		return combined;
	}
	
	private static int compareOnes(HashMap<Character, Integer> term1, HashMap<Character, Integer> term2) {
		return numberOfOnes(term1).compareTo(numberOfOnes(term2));
	}
	private static Integer numberOfOnes(HashMap<Character, Integer> term) {
		int count = 0;
		for(Integer i : term.values()) {
			count += i == 1 ? 1 : 0;
		}
		return count;
	}
	
	private static String expressionToString(LinkedList<HashMap<Character, Integer>> terms) {
		String s = "";
		for(HashMap<Character, Integer> term : terms) {
			s += termToString(term) + " + ";
		}
		return s.substring(0, s.length()-2);
	}
	private static Integer rateTermOrder(HashMap<Character, Integer> term) {
		int ranking = 0;
		for(char character : termToString(term).toCharArray()) {
			if(character != '~') {
				ranking += Character.getNumericValue(character);
			}
		}
		return ranking;
	}
	private static String termToString(HashMap<Character, Integer> term) {
		String s = "";
		for(Character key : term.keySet()) {
			s += (term.get(key) == 0 ? "~" : "") + key;
		}
		return s;
	}
	private static HashMap<Character, Integer> stringToTermMap(String termString) {
		HashMap<Character, Integer> termMap = new HashMap<>();
		
		for(int j = 0; j < termString.length(); j++) {
			char charAtJ = termString.charAt(j);
			int bitValue = 1;
			if(charAtJ == '~') {
				j++;
				charAtJ = termString.charAt(j);
				bitValue = 0;
			}
			
			if(termMap.keySet().contains(charAtJ)) {
				int otherBit = termMap.get(charAtJ);
				if(bitValue != otherBit) {
					termMap.remove(charAtJ);
				}
			} else {
				termMap.put(charAtJ, bitValue);
			}
		}
		
		return termMap;
	}

}