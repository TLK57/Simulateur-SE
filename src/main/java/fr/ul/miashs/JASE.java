package fr.ul.miashs;

public class JASE {
    public static void main (String[] args){

        if (args.length < 2) {
            System.err.println("Usage: java Ordonnanceur <fichier_config> <fichier_programmes>");
            return;
        }
        String configPath = args[0];
        String programsPath = args[1];

        Ordonnanceur ordonnanceur = new Ordonnanceur();
        ordonnanceur.executerDepuisFichiers(configPath,programsPath);
    }
}
