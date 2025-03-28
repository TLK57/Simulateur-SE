package fr.ul.miashs;
import java.util.Arrays;
import java.util.List;
public class Ordonnanceur {
    // Ajout de l'attribut pour la gestion mémoire
    private static GestionMemoire gestionMemoire;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java Ordonnanceur <fichier_config> <fichier_programmes>");
            return;
        }
        String configPath = args[0];
        String programsPath = args[1];

        // Paramètres de configuration
        int tempsSimulation = 0;
        int interruptionHorloge = 0;
        String strategieOrdonnancement = "";
        int quantum = 0;
        int tempsEcritureDisque = 0;
        int tempsChargePage = 0;
        String politiqueAllocation = "locale";
        int nombreCadresLocal = 4;
        int nombreCadresTotal = 8;
        String algorithmePagement = "FIFO";

        // Lecture du fichier de configuration
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(configPath))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                ligne = ligne.trim();
                if (ligne.isEmpty() || ligne.startsWith("#")) {
                    continue;
                }
                String[] parts;
                if (ligne.contains("=")) {
                    parts = ligne.split("=");
                } else if (ligne.contains(":")) {
                    parts = ligne.split(":");
                } else {
                    parts = new String[]{ ligne };
                }
                if (parts.length < 1) continue;
                String cle = parts[0].trim();
                String valeur = (parts.length > 1 ? parts[1].trim() : "");
                switch (cle.toLowerCase()) {
                    case "temps-simulation":
                        tempsSimulation = Integer.parseInt(valeur);
                        break;
                    case "interruption-horloge":
                        interruptionHorloge = Integer.parseInt(valeur);
                        break;
                    case "processus-ordonnancement":
                        strategieOrdonnancement = valeur;
                        break;
                    case "processus-quantum":
                        quantum = Integer.parseInt(valeur);
                        break;
                    case "temps-ecriture-disque":
                        tempsEcritureDisque = Integer.parseInt(valeur);
                        break;
                    case "temps-charge-page":
                        tempsChargePage = Integer.parseInt(valeur);
                        break;
                    case "pagination-politique-allocation":
                        politiqueAllocation = valeur;
                        break;
                    case "pagination-nombre-cadres-locale":
                        nombreCadresLocal = Integer.parseInt(valeur);
                        break;
                    case "pagination-nombre-cadres":
                        nombreCadresTotal = Integer.parseInt(valeur);
                        break;
                    case "pagination-algorithme":
                        algorithmePagement = valeur;
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture du fichier de configuration : " + e.getMessage());
            return;
        }

        // Initialisation du gestionnaire de mémoire
        gestionMemoire = new GestionMemoire(
            nombreCadresTotal,
            nombreCadresLocal,
            politiqueAllocation.equalsIgnoreCase("locale"),
            algorithmePagement,
            tempsChargePage
        );

        // Vérification de la politique d'ordonnancement (FIFO attendu)
        if (!strategieOrdonnancement.equalsIgnoreCase("FIFO")) {
            System.out.println("Stratégie d'ordonnancement non prise en charge : " + strategieOrdonnancement);
            System.out.println("Veuillez utiliser FIFO dans le fichier de configuration pour la Partie I.");
            return;
        }

        // Lecture du fichier de programmes (processus à simuler)
        java.util.List<Processus> listeProcessus = new java.util.ArrayList<>();
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(programsPath))) {
            String ligne;
            while ((ligne = br.readLine()) != null) {
                ligne = ligne.trim();
                if (ligne.isEmpty() || ligne.startsWith("#")) continue;
                // Format attendu : ID/Temps-en-ms/Priorité/{EVENEMENT(valeur),...}
                String[] parts = ligne.split("/", 4);
                if (parts.length < 4) continue; // format incorrect
                String id = parts[0].trim();
                int tempsArrivee = Integer.parseInt(parts[1].trim());
                int priorite = Integer.parseInt(parts[2].trim());
                String sequenceEvenements = parts[3].trim();
                // Retirer les accolades englobantes
                if (sequenceEvenements.startsWith("{")) {
                    sequenceEvenements = sequenceEvenements.substring(1);
                }
                if (sequenceEvenements.endsWith("}")) {
                    sequenceEvenements = sequenceEvenements.substring(0, sequenceEvenements.length() - 1);
                }
                String[] evtParts = sequenceEvenements.split(",");
                java.util.List<Evenement> evenements = new java.util.ArrayList<>();
                for (String evtStr : evtParts) {
                    evtStr = evtStr.trim();
                    if (evtStr.isEmpty()) continue;
                    // evtStr est de la forme CODE(valeur)
                    int parOuv = evtStr.indexOf('(');
                    int parFer = evtStr.lastIndexOf(')');
                    String code = (parOuv >= 0 ? evtStr.substring(0, parOuv) : evtStr);
                    String argStr = (parOuv >= 0 && parFer >= 0 ? evtStr.substring(parOuv + 1, parFer) : "");
                    int arg = 0;
                    try {
                        arg = Integer.parseInt(argStr.trim());
                    } catch (NumberFormatException ex) {
                        arg = 0;
                    }
                    TypeEvenement type;
                    try {
                        type = TypeEvenement.valueOf(code.toUpperCase());
                    } catch (IllegalArgumentException ex) {
                        // on s'en fiche des événement inconnu
                        continue;
                    }
                    evenements.add(new Evenement(type, arg));
                }
                listeProcessus.add(new Processus(id, tempsArrivee, priorite, evenements));
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la lecture du fichier de programmes : " + e.getMessage());
            return;
        }

        // on trie les processus par ordre d'arrivée croissant (FIFO se base sur l'arrivée)
        listeProcessus.sort(java.util.Comparator.comparingInt(p -> p.tempsArrivee));

        // File d'attente des processus prêts (ordonnancement FIFO)
        java.util.LinkedList<Processus> filePrets = new java.util.LinkedList<>();
        // Liste des processus bloqués (triée par l'instant de fin de blocage le plus proche)
        java.util.PriorityQueue<Processus> fileBloques = new java.util.PriorityQueue<>(java.util.Comparator.comparingInt(p -> p.finBlocage));
        int indexArrivee = 0;             // index du prochain processus à arriver
        int tempsCourant = 0;            // temps simulé actuel en millisecondes
        Processus processusEnCours = null; // processus actuellement exécuté sur le CPU
        int nbTermines = 0;
        long sommeDureesRotation = 0;
        long sommeTempsReponse = 0;

        // Ajouter les processus arrivant à t=0 à la file des prêts
        while (indexArrivee < listeProcessus.size() && listeProcessus.get(indexArrivee).tempsArrivee == 0) {
            filePrets.add(listeProcessus.get(indexArrivee));
            indexArrivee++;
        }

        // Début de la simulation
        while (tempsCourant < tempsSimulation) {
            // Mettre à jour la file de prêts avec les nouvelles arrivées jusqu'à tempsCourant
            while (indexArrivee < listeProcessus.size() && listeProcessus.get(indexArrivee).tempsArrivee <= tempsCourant) {
                filePrets.add(listeProcessus.get(indexArrivee));
                indexArrivee++;
            }
            // Mettre à jour la file de prêts avec les processus dont le blocage est terminé à tempsCourant
            while (!fileBloques.isEmpty() && fileBloques.peek().finBlocage <= tempsCourant) {
                filePrets.add(fileBloques.poll());
            }

            if (processusEnCours == null) {
                // CPU est inactif
                if (!filePrets.isEmpty()) {
                    // Un processus est prêt à être exécuté
                    Processus prochain = filePrets.poll();
                    // Si on n'est pas aligné sur un tick d'horloge, attendre le prochain multiple d'interruptionHorloge
                    if (interruptionHorloge > 0 && (tempsCourant % interruptionHorloge != 0)) {
                        int prochainTick = ((tempsCourant / interruptionHorloge) + 1) * interruptionHorloge;
                        if (prochainTick > tempsSimulation) {
                            prochainTick = tempsSimulation;
                        }
                        if (prochainTick > tempsCourant) {
                            System.out.println(tempsCourant + " - " + prochainTick + " : Inactivité");
                            tempsCourant = prochainTick;
                        }
                    }
                    if (tempsCourant >= tempsSimulation) break;
                    // Démarrer l'exécution du processus sélectionné
                    processusEnCours = prochain;
                    if (processusEnCours.debutExecution < 0) {
                        processusEnCours.debutExecution = tempsCourant;
                    }
                    // Noter le début de période de traitement pour la trace
                    int debutSegment = tempsCourant;
                    // Exécuter le processus courant (on traitera son événement dans le bloc suivant)
                    continue;
                } else {
                    // Aucun processus prêt -> rester en inactivité jusqu'au prochain événement (arrivée ou fin de blocage)
                    int prochainEvt = tempsSimulation;
                    if (indexArrivee < listeProcessus.size()) {
                        prochainEvt = Math.min(prochainEvt, listeProcessus.get(indexArrivee).tempsArrivee);
                    }
                    if (!fileBloques.isEmpty()) {
                        prochainEvt = Math.min(prochainEvt, fileBloques.peek().finBlocage);
                    }
                    if (prochainEvt > tempsCourant) {
                        if (prochainEvt > tempsSimulation) prochainEvt = tempsSimulation;
                        System.out.println(tempsCourant + " - " + prochainEvt + " : Inactivité");
                        tempsCourant = prochainEvt;
                    } else {
                        // prochainEvt == tempsCourant (un événement se produit maintenant, déjà géré ci-dessus)
                        tempsCourant++;
                    }
                    continue;
                }
            }

            // Un processus est en cours d'exécution sur le CPU
            Evenement evt = processusEnCours.prochainEvenement();
            if (evt == null) {
                // Plus d'événements (devrait normalement être géré par FIN)
                processusEnCours = null;
                continue;
            }
            switch (evt.type) {
                case CALCUL: {
                    // Exécution d'un calcul CPU de durée evt.valeur ms
                    int duree = evt.valeur;
                    int finPrevue = tempsCourant + duree;
                    // Simuler le calcul en avançant le temps, sans interruption par l'ordonnanceur (non préemptif)
                    while (true) {
                        // Prochain événement (arrivée ou fin de blocage) avant la fin du calcul ?
                        int prochainEvtExterne = finPrevue;
                        if (indexArrivee < listeProcessus.size()) {
                            prochainEvtExterne = Math.min(prochainEvtExterne, listeProcessus.get(indexArrivee).tempsArrivee);
                        }
                        if (!fileBloques.isEmpty()) {
                            prochainEvtExterne = Math.min(prochainEvtExterne, fileBloques.peek().finBlocage);
                        }
                        if (prochainEvtExterne >= finPrevue || prochainEvtExterne > tempsSimulation) {
                            // Aucun événement externe avant la fin du calcul, ou simulation se termine avant
                            int finSegment = Math.min(finPrevue, tempsSimulation);
                            tempsCourant = finSegment;
                            // Fin du calcul ou de la portion de calcul
                            System.out.println((tempsCourant - duree) + " - " + tempsCourant + " : Processus " + processusEnCours.id);
                            // Consommer l'événement CALCUL traité
                            processusEnCours.avancerEvenement();
                            // Mettre à jour le début du prochain segment de traitement au même instant (finSegment)
                            int debutSegment = tempsCourant;
                            // Si la simulation se termine exactement ici, on sort
                            if (tempsCourant >= tempsSimulation) {
                                processusEnCours = null;
                                break;
                            }
                            // Le calcul est terminé, on sort de la boucle de calcul
                            break;
                        }
                        // S'il y a un événement externe avant la fin du calcul, avancer le temps jusqu'à cet événement
                        tempsCourant = prochainEvtExterne;
                        // Mettre à jour les arrivées et fin de blocage à tempsCourant
                        while (indexArrivee < listeProcessus.size() && listeProcessus.get(indexArrivee).tempsArrivee <= tempsCourant) {
                            filePrets.add(listeProcessus.get(indexArrivee));
                            indexArrivee++;
                        }
                        while (!fileBloques.isEmpty() && fileBloques.peek().finBlocage <= tempsCourant) {
                            filePrets.add(fileBloques.poll());
                        }
                        // Continuer le calcul (pas de changement de processus, non préemptif)
                        continue;
                    }
                    if (tempsCourant >= tempsSimulation) {
                        // Fin de simulation atteinte pendant le calcul
                        break;
                    }
                    // Le calcul est terminé. Continuer avec le même processus (il n'est pas encore bloqué ou terminé).
                    continue;
                }
                case LECTURE:
                    // Tentative d'accès à la page
                    String processusId = processusEnCours.id;
                    if (!gestionMemoire.accederPage(processusId, evt.valeur)) {
                        // Défaut de page - bloquer le processus pendant le temps de chargement
                        processusEnCours.finBlocage = tempsCourant + tempsChargePage;
                        fileBloques.add(processusEnCours);
                        StringBuilder sb = new StringBuilder();
                        sb.append(tempsCourant).append(" : Défaut de page (LECTURE) - Processus ").append(processusId).append(" bloqué");
                        System.out.println(sb.toString());
                        processusEnCours = null;
                        continue;
                    }
                    processusEnCours.avancerEvenement();
                    break;
                case ECRITURE:
                    // Tentative d'accès à la page
                    processusId = processusEnCours.id;
                    if (!gestionMemoire.accederPage(processusId, evt.valeur)) {
                        // Défaut de page - bloquer le processus pendant le temps de chargement
                        processusEnCours.finBlocage = tempsCourant + tempsChargePage;
                        fileBloques.add(processusEnCours);
                        StringBuilder sb = new StringBuilder();
                        sb.append(tempsCourant).append(" : Défaut de page (ECRITURE) - Processus ").append(processusId).append(" bloqué");
                        System.out.println(sb.toString());
                        processusEnCours = null;
                        continue;
                    }
                    // Simuler l'écriture sur le disque
                    processusEnCours.finBlocage = tempsCourant + tempsEcritureDisque;
                    fileBloques.add(processusEnCours);
                    processusEnCours = null;
                    break;
                case DORMIR: {
                    // Événement entraînant un blocage du processus
                    int dureeBlocage = evt.valeur;
                    // Consommer l'événement de blocage
                    processusEnCours.avancerEvenement();
                    // Déterminer quand le processus sera à nouveau prêt
                    processusEnCours.finBlocage = tempsCourant + dureeBlocage;
                    // Insérer le processus dans la liste des bloqués
                    fileBloques.add(processusEnCours);
                    // Libérer le CPU (processus passe à l'état Bloqué)
                    processusEnCours = null;
                    continue;
                }
                case FIN: {
                    // Fin d'exécution du processus
                    processusEnCours.avancerEvenement();
                    // Collecter les métriques de performance
                    nbTermines++;
                    sommeDureesRotation += (tempsCourant - processusEnCours.tempsArrivee);
                    sommeTempsReponse += (processusEnCours.debutExecution - processusEnCours.tempsArrivee);
                    // Retirer le processus (terminé)
                    processusEnCours = null;
                    continue;
                }
            }
        }

        // Fin de la simulation : affichage du rapport final
        System.out.println("Nombre de processus terminés = " + nbTermines);
        if (nbTermines > 0) {
            double delaiRotationMoyen = (double) sommeDureesRotation / nbTermines;
            double reactiviteMoyenne = (double) sommeTempsReponse / nbTermines;
            System.out.printf("Délai de rotation moyen = %.2f ms\n", delaiRotationMoyen);
            System.out.printf("Réactivité moyenne = %.2f ms\n", reactiviteMoyenne);
        } else {
            System.out.println("Délai de rotation moyen = 0 ms");
            System.out.println("Réactivité moyenne = 0 ms");
        }

        // À la fin de la simulation, afficher les statistiques de la mémoire
        System.out.println("\nStatistiques de la gestion mémoire :");
        System.out.println(gestionMemoire.getTraceMemoire());
    }

    // Représentation d'un événement d'un processus
    static class Evenement {
        TypeEvenement type;
        int valeur;
        Evenement(TypeEvenement type, int valeur) {
            this.type = type;
            this.valeur = valeur;
        }
    }

    // Types d'événements possibles
    enum TypeEvenement { CALCUL, ECRITURE, LECTURE, DORMIR, FIN }

    // Représentation d'un processus
    static class Processus {
        String id;
        int tempsArrivee;
        int priorite;
        java.util.List<Evenement> evenements;
        int indexEvenement;
        int finBlocage;
        int debutExecution;
        Processus(String id, int tempsArrivee, int priorite, java.util.List<Evenement> evenements) {
            this.id = id;
            this.tempsArrivee = tempsArrivee;
            this.priorite = priorite;
            this.evenements = evenements;
            this.indexEvenement = 0;
            this.finBlocage = 0;
            this.debutExecution = -1;
        }
        Evenement prochainEvenement() {
            return (indexEvenement < evenements.size() ? evenements.get(indexEvenement) : null);
        }
        void avancerEvenement() {
            indexEvenement++;
        }
    }
}
