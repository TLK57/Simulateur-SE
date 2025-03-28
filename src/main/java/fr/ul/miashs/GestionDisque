package fr.ul.miashs;

import java.util.*;
import java.io.FileWriter;
import java.io.IOException;

public class GestionDisque {

    private List<Integer> requetes; // Liste des requêtes de lecture
    private int positionBras; // Position actuelle du bras du disque
    private String direction; // Sens de déplacement du bras ("gauche" ou "droite")

    // Constructeur pour initialiser les requêtes, la position du bras et la direction
    public GestionDisque(List<Integer> requetes, int positionInitiale, String direction) {
        this.requetes = new ArrayList<>(requetes); // Copie des requêtes pour éviter les modifications externes
        this.positionBras = positionInitiale; // Position initiale du bras
        this.direction = direction; // Direction initiale du bras
    }

    // 1 FIFO (First In First Out) : Sert les requêtes dans l'ordre d'arrivée
    public int fifo() {
        int deplacementTotal = 0; // Variable pour stocker le déplacement total du bras
        int positionActuelle = positionBras; // Position actuelle du bras

        // Parcours de toutes les requêtes dans l'ordre
        for (int r : requetes) {
            deplacementTotal += Math.abs(r - positionActuelle); // Calcul du déplacement entre la position actuelle et la requête
            positionActuelle = r; // Mise à jour de la position actuelle du bras
        }
        return deplacementTotal; // Retour du déplacement total
    }

    // 2️ SSF (Shortest Seek First) : Sert la requête la plus proche du bras en priorité
    public int ssf() {
        int deplacementTotal = 0;
        int positionActuelle = positionBras;
        List<Integer> aTraiter = new ArrayList<>(requetes); // Copie de la liste des requêtes à traiter

        // Tant qu'il reste des requêtes à traiter
        while (!aTraiter.isEmpty()) {
            int prochain = trouverPlusProche(positionActuelle, aTraiter); // Trouve la requête la plus proche
            deplacementTotal += Math.abs(prochain - positionActuelle); // Calcul du déplacement
            positionActuelle = prochain; // Mise à jour de la position du bras
            aTraiter.remove(Integer.valueOf(prochain)); // Suppression de la requête traitée
        }
        return deplacementTotal;
    }

    // Méthode auxiliaire pour trouver la requête la plus proche
    private int trouverPlusProche(int position, List<Integer> liste) {
        return liste.stream().min(Comparator.comparingInt(r -> Math.abs(r - position))).orElse(position);
    }

    // 3️ SCAN (Algorithme de l'ascenseur)
    public int scan() {
        List<Integer> copieRequetes = new ArrayList<>(requetes); // Copie des requêtes
        copieRequetes.add(positionBras); // Ajout de la position initiale du bras dans la liste

        // Ajout des bornes 0 et 199 si elles ne sont pas déjà présentes
        if (!copieRequetes.contains(0)) copieRequetes.add(0);
        if (!copieRequetes.contains(199)) copieRequetes.add(199);

        Collections.sort(copieRequetes); // Tri des requêtes pour parcourir dans l'ordre
        int index = copieRequetes.indexOf(positionBras); // Trouve la position du bras après tri
        int deplacementTotal = 0;
        int positionActuelle = positionBras;
        List<Integer> sequence = new ArrayList<>(); // Liste pour afficher la séquence de traitement

        if ("droite".equals(direction)) {
            // Déplacement vers la droite
            for (int i = index; i < copieRequetes.size(); i++) {
                sequence.add(copieRequetes.get(i));
                deplacementTotal += Math.abs(copieRequetes.get(i) - positionActuelle);
                positionActuelle = copieRequetes.get(i);
            }
            // Retour vers la gauche
            for (int i = index - 1; i >= 0; i--) {
                sequence.add(copieRequetes.get(i));
                deplacementTotal += Math.abs(copieRequetes.get(i) - positionActuelle);
                positionActuelle = copieRequetes.get(i);
            }
        } else {
            // Déplacement vers la gauche
            for (int i = index; i >= 0; i--) {
                sequence.add(copieRequetes.get(i));
                deplacementTotal += Math.abs(copieRequetes.get(i) - positionActuelle);
                positionActuelle = copieRequetes.get(i);
            }
            // Retour vers la droite
            for (int i = index + 1; i < copieRequetes.size(); i++) {
                sequence.add(copieRequetes.get(i));
                deplacementTotal += Math.abs(copieRequetes.get(i) - positionActuelle);
                positionActuelle = copieRequetes.get(i);
            }
        }

        System.out.println("SCAN Séquence : " + sequence);
        return deplacementTotal;
    }

    // 4️ CSCAN (Circular SCAN)
    public int cscan() {
        List<Integer> copieRequetes = new ArrayList<>(requetes);
        copieRequetes.add(positionBras);

        // Ajout des bornes 0 et 199 si elles ne sont pas déjà présentes
        if (!copieRequetes.contains(0)) copieRequetes.add(0);
        if (!copieRequetes.contains(199)) copieRequetes.add(199);

        Collections.sort(copieRequetes);
        int index = copieRequetes.indexOf(positionBras);
        int deplacementTotal = 0;
        int positionActuelle = positionBras;
        List<Integer> sequence = new ArrayList<>();

        // Parcours vers la droite jusqu'à la fin (199)
        for (int i = index; i < copieRequetes.size(); i++) {
            sequence.add(copieRequetes.get(i));
            deplacementTotal += Math.abs(copieRequetes.get(i) - positionActuelle);
            positionActuelle = copieRequetes.get(i);
        }

        // Retour circulaire à 0 (uniquement si ce n'est pas déjà le cas)
        if (sequence.get(sequence.size() - 1) != 0) {
            deplacementTotal += Math.abs(199 - 0);
            sequence.add(0);
        }
        positionActuelle = 0;

        // Reprendre du début et traiter les requêtes restantes
        for (int i = 0; i < index; i++) {
            sequence.add(copieRequetes.get(i));
            deplacementTotal += Math.abs(copieRequetes.get(i) - positionActuelle);
            positionActuelle = copieRequetes.get(i);
        }

        System.out.println("CSCAN Séquence : " + sequence);
        return deplacementTotal;
    }

    // Génération du fichier log avec les résultats
    public void genererLog(String nomFichier, String algo, int deplacementTotal) {
        try (FileWriter writer = new FileWriter(nomFichier, true)) {
            writer.write(algo + " - Déplacement total : " + deplacementTotal + " pistes\n");
            writer.write("Requêtes traitées : " + requetes.toString() + "\n");
            writer.write("--------------------------------\n");
        } catch (IOException e) {
            System.err.println("Erreur lors de l'écriture du fichier log : " + e.getMessage());
        }
    }
}
