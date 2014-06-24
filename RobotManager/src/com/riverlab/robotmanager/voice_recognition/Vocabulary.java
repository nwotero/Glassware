package com.riverlab.robotmanager.voice_recognition;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class Vocabulary 
{
	ArrayList<String> defaultSubVocabs;
	private HashMap<String, ArrayList<Phrase>> subVocabs;

	public Vocabulary (String rawXML)
	{
		//Create the builder factory
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;

		//Create the builder from the builder factory
		try {
			builder = builderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();  
		}

		//Use InputSource and StringReader to represent the rawXML String object
		//as a file and parse it using the builder. 
		Document document = null;
		try {
			InputSource is = new InputSource(new StringReader(rawXML));
			document = builder.parse(is);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		//Get the defualt sub-vocabs from the root vocab elemement's attributes
		Element rootElement = document.getDocumentElement();
		String defaultSubVocabs = rootElement.getAttribute("default");
		String[] defaultSubVocabArray = defaultSubVocabs.split(",");
		this.defaultSubVocabs = (ArrayList<String>) Arrays.asList(defaultSubVocabArray);

		NodeList subVocabs = rootElement.getChildNodes();
		HashMap<String, ArrayList<Phrase>> subVocabMap = new HashMap<String, ArrayList<Phrase>>();

		for (int i = 0; i < subVocabs.getLength(); i++)
		{
			Element subVocab = (Element) subVocabs.item(i);
			String subVocabName = subVocab.getAttribute("name");

			NodeList phraseNodes = subVocab.getChildNodes();
			ArrayList<Phrase> phrases = new ArrayList<Phrase>();
			for (int j = 0; j < phraseNodes.getLength(); j++)
			{
				Element phraseNode = (Element) phraseNodes.item(i);

				String tempName = phraseNode.getAttribute("text");

				NodeList modifiers = phraseNode.getChildNodes();

				if (modifiers.getLength() == 0)
				{
					phrases.add(new Phrase(tempName, new ArrayList<Modifier>()));
				}
				else
				{
					ArrayList<Modifier> tempModList = new ArrayList<Vocabulary.Modifier>();
					for (int k = 0; k < modifiers.getLength(); k++)
					{
						//Construct the remainder depth of this tree using 
						//recursive modifier constructor
						Element mod = (Element)modifiers.item(k);
						tempModList.add(new Modifier(mod));
					}
					phrases.add(new Phrase(tempName, tempModList));
				}
			}
			subVocabMap.put(subVocabName, phrases);
		}
	}

	public Phrase findPhrase(String phraseText)
	{
		//Search sub-vocabularies for phrase
		for (Map.Entry<String, ArrayList<Phrase>> subVocab : subVocabs.entrySet())
		{
			ArrayList<Phrase> phrases = subVocab.getValue();

			//Search through phrases in sub-vocabulary for phrase
			for (Phrase phrase : phrases)
			{
				if (phrase.text.equals(phraseText))
				{
					return phrase;
				}
			}
		}
		//No such phrase
		return null;
	}

	public Modifier findModifier(String phraseText, ArrayList<Modifier> modifiers)
	{
		String modifierUses = "";

		//Search sub-vocabularies for phrase
		searchloop:
			for (Map.Entry<String, ArrayList<Phrase>> subVocab : subVocabs.entrySet())
			{
				ArrayList<Phrase> phrases = subVocab.getValue();

				//Search through phrases in sub-vocabulary for phrase
				for (Phrase phrase : phrases)
				{
					if (phrase.text.equals(phraseText))
					{
						modifierUses = subVocab.getKey();
						break searchloop;
					}
				}
			}			

		for (Modifier mod : modifiers)
		{
			if (mod.getUses() == modifierUses)
			{
				return mod;
			}
		}

		return null;
	}

	public NewPhrasesMessage getNextPhrases(ArrayList<String> previous)
	{
		ArrayList<String> rtnList = new ArrayList<String>();
		boolean isRequired = true;
		boolean reset = false;

		if (previous.size() == 0)
		{
			for (String subVocabName : defaultSubVocabs)
			{
				for (Phrase phrase : subVocabs.get(subVocabName))
				{
					rtnList.add(phrase.getText());
				}
			}
		}
		else
		{
			Phrase rootPhrase = findPhrase(previous.remove(0));
			VocabMessage msg = rootPhrase.getNextSubVocabs(previous);
			ArrayList<String> nextSubVocabs = msg.subVocabNames;
			isRequired = msg.isRequired();

			if (nextSubVocabs.size() == 0)
			{
				for (String subVocabName : defaultSubVocabs)
				{
					for (Phrase phrase : subVocabs.get(subVocabName))
					{
						rtnList.add(phrase.getText());
					}
				}
				reset = true;
			}
			else
			{			
				for (String subVocabName : nextSubVocabs)
				{
					for (Phrase phrase : subVocabs.get(subVocabName))
					{
						rtnList.add(phrase.getText());
					}
				}
			}
		}
		return new NewPhrasesMessage(rtnList, isRequired, reset);
	}



	public class Phrase
	{
		private String text;
		private boolean isRequired = true;
		private ArrayList<Modifier> modifiers;

		public Phrase(String text, ArrayList<Modifier> modifiers)
		{
			this.text = text;
			this.modifiers = modifiers;
		}

		public String getText()
		{
			return text;
		}

		public boolean isRequired()
		{
			return isRequired;
		}

		public ArrayList<Modifier> getModifiers()
		{
			return modifiers;
		}

		public void setText(String text)
		{
			this.text = text;
		}

		public void setRequired(boolean required)
		{
			this.isRequired = required;
		}

		public void setModifiers(ArrayList<Modifier> modifiers)
		{
			this.modifiers = modifiers;
		}

		public VocabMessage getNextSubVocabs(ArrayList<String> previous)
		{
			ArrayList<String> rtnList = new ArrayList<String>();

			if (previous.size() == 0)
			{
				if (modifiers.size() == 0)
				{
					return new VocabMessage(rtnList, false);
				}
				else
				{
					boolean required = false;
					for (Modifier mod : modifiers)
					{
						rtnList.add(mod.getUses());
						required |= mod.isRequired();
					}
					return new VocabMessage(rtnList, required);
				}
			}
			else
			{
				Modifier usedMod = findModifier(previous.remove(0), modifiers);
				return usedMod.getNextSubVocabs(previous);
			}
		}
	}

	public class Modifier
	{
		private String uses;
		private boolean isRequired;
		private ArrayList<Modifier> modifiers;

		public Modifier(Element modifierNode)
		{
			this.uses = modifierNode.getAttribute("uses");
			this.isRequired = modifierNode.getAttribute("required") != "false";

			NodeList additionalMods = modifierNode.getChildNodes();

			if (additionalMods.getLength() == 0)
			{
				this.modifiers = new ArrayList<Vocabulary.Modifier>();
			}
			else
			{
				ArrayList<Modifier> tempList = new ArrayList<Vocabulary.Modifier>();
				for (int i = 0; i < additionalMods.getLength(); i++)
				{
					Element elm = (Element)additionalMods.item(i);

					tempList.add(new Modifier(elm));
				}
			}
		}

		public String getUses()
		{
			return uses;
		}

		public boolean isRequired()
		{
			return isRequired;
		}

		public ArrayList<Modifier> getModifiers()
		{
			return modifiers;
		}

		public void setUses(String uses)
		{
			this.uses = uses;
		}

		public void setRequired(boolean required)
		{
			isRequired = required;
		}

		public void setModifiers(ArrayList<Modifier> modifiers)
		{
			this.modifiers = modifiers;
		}

		public VocabMessage getNextSubVocabs(ArrayList<String> previous)
		{
			ArrayList<String> rtnList = new ArrayList<String>();

			if (previous.size() == 0)
			{
				if (modifiers.size() == 0)
				{
					return new VocabMessage(rtnList, false);
				}
				else
				{
					boolean required = false;
					for (Modifier mod : modifiers)
					{
						rtnList.add(mod.getUses());
						required |= mod.isRequired();
					}

					return new VocabMessage(rtnList, required);
				}
			}
			else
			{
				Modifier usedMod = findModifier(previous.remove(0), modifiers);
				return usedMod.getNextSubVocabs(previous);
			}
		}
	}

	public class VocabMessage
	{
		private ArrayList<String> subVocabNames;
		private boolean isRequired;

		public VocabMessage(ArrayList<String> subVocabNames, boolean isRequired)
		{
			this.subVocabNames = subVocabNames;
			this.isRequired = isRequired;
		}

		public ArrayList<String> getSubVocabNames() 
		{
			return subVocabNames;
		}

		public void setSubVocabNames(ArrayList<String> subVocabNames) 
		{
			this.subVocabNames = subVocabNames;
		}

		public boolean isRequired() 
		{
			return isRequired;
		}

		public void setRequired(boolean isRequired) 
		{
			this.isRequired = isRequired;
		}

	}
}
