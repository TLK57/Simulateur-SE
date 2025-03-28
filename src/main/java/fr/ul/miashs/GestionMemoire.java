package fr.ul.miashs;

import java.util.*;

public class GestionMemoire {
    // Constantes pour les algorithmes de remplacement
    public enum AlgorithmeRemplacement {
        FIFO,
        NRU,
        SECONDE_CHANCE,
        OPTIMAL
    }

    // Structure pour représenter une page en mémoire
    private static class Page {
        int numeroPage;
        String processusId;
        boolean referenced;
        boolean modified;
        long lastAccess;
        
        public Page(int numeroPage, String processusId) {
            this.numeroPage = numeroPage;
            this.processusId = processusId;
            this.referenced = false;
            this.modified = false;
            this.lastAccess = System.currentTimeMillis();
        }
    }

    // Attributs de la classe
    private final int nombreCadresTotal;
    private final int nombreCadresLocal;
    private final boolean allocationLocale;
    private final AlgorithmeRemplacement algorithme;
    private final Map<String, List<Page>> cadresParProcessus;
    private final List<Page> cadresGlobaux;
    private final Map<String, Queue<Page>> fifoQueues;
    private int defautsDePage;
    private final int tempsChargePage;

    // Constructeur
    public GestionMemoire(int nombreCadresTotal, int nombreCadresLocal, boolean allocationLocale, 
                         String algorithme, int tempsChargePage) {
        this.nombreCadresTotal = nombreCadresTotal;
        this.nombreCadresLocal = nombreCadresLocal;
        this.allocationLocale = allocationLocale;
        this.algorithme = AlgorithmeRemplacement.valueOf(algorithme.toUpperCase());
        this.tempsChargePage = tempsChargePage;
        this.cadresParProcessus = new HashMap<>();
        this.cadresGlobaux = new ArrayList<>();
        this.fifoQueues = new HashMap<>();
        this.defautsDePage = 0;
    }

    // Méthode pour accéder à une page
    public boolean accederPage(String processusId, int numeroPage) {
        // Vérifier si la page est déjà en mémoire
        if (estEnMemoire(processusId, numeroPage)) {
            mettreAJourAcces(processusId, numeroPage);
            return true;
        }

        // Défaut de page
        defautsDePage++;
        
        // Gérer le défaut de page selon la politique d'allocation
        if (allocationLocale) {
            return gererDefautPageLocal(processusId, numeroPage);
        } else {
            return gererDefautPageGlobal(processusId, numeroPage);
        }
    }

    // Vérifier si une page est en mémoire
    private boolean estEnMemoire(String processusId, int numeroPage) {
        if (allocationLocale) {
            List<Page> cadresProcessus = cadresParProcessus.get(processusId);
            if (cadresProcessus != null) {
                return cadresProcessus.stream()
                    .anyMatch(p -> p.numeroPage == numeroPage && p.processusId.equals(processusId));
            }
            return false;
        } else {
            return cadresGlobaux.stream()
                .anyMatch(p -> p.numeroPage == numeroPage && p.processusId.equals(processusId));
        }
    }

    // Gérer un défaut de page en allocation locale
    private boolean gererDefautPageLocal(String processusId, int numeroPage) {
        List<Page> cadresProcessus = cadresParProcessus.computeIfAbsent(processusId, k -> new ArrayList<>());
        
        // Si il y a de la place disponible
        if (cadresProcessus.size() < nombreCadresLocal) {
            Page nouvellePage = new Page(numeroPage, processusId);
            cadresProcessus.add(nouvellePage);
            if (algorithme == AlgorithmeRemplacement.FIFO) {
                fifoQueues.computeIfAbsent(processusId, k -> new LinkedList<>()).add(nouvellePage);
            }
            return true;
        }

        // Sinon, appliquer l'algorithme de remplacement
        return remplacerPage(processusId, numeroPage, cadresProcessus);
    }

    // Gérer un défaut de page en allocation globale
    private boolean gererDefautPageGlobal(String processusId, int numeroPage) {
        // Si il y a de la place disponible
        if (cadresGlobaux.size() < nombreCadresTotal) {
            Page nouvellePage = new Page(numeroPage, processusId);
            cadresGlobaux.add(nouvellePage);
            if (algorithme == AlgorithmeRemplacement.FIFO) {
                fifoQueues.computeIfAbsent("global", k -> new LinkedList<>()).add(nouvellePage);
            }
            return true;
        }

        // Sinon, appliquer l'algorithme de remplacement
        return remplacerPage(processusId, numeroPage, cadresGlobaux);
    }

    // Remplacer une page selon l'algorithme choisi
    private boolean remplacerPage(String processusId, int numeroPage, List<Page> cadres) {
        Page pageARemplacer = null;
        
        switch (algorithme) {
            case FIFO:
                pageARemplacer = remplacementFIFO(processusId);
                break;
            case NRU:
                pageARemplacer = remplacementNRU(cadres);
                break;
            case SECONDE_CHANCE:
                pageARemplacer = remplacementSecondeChance(cadres);
                break;
            case OPTIMAL:
                pageARemplacer = remplacementOptimal(cadres);
                break;
        }

        if (pageARemplacer != null) {
            // Remplacer la page
            int index = cadres.indexOf(pageARemplacer);
            Page nouvellePage = new Page(numeroPage, processusId);
            cadres.set(index, nouvellePage);
            
            if (algorithme == AlgorithmeRemplacement.FIFO) {
                Queue<Page> queue = fifoQueues.get(allocationLocale ? processusId : "global");
                queue.remove(pageARemplacer);
                queue.add(nouvellePage);
            }
            
            return true;
        }
        
        return false;
    }

    // Implémentation de l'algorithme FIFO
    private Page remplacementFIFO(String processusId) {
        Queue<Page> queue = fifoQueues.get(allocationLocale ? processusId : "global");
        return queue != null ? queue.peek() : null;
    }

    // Implémentation de l'algorithme NRU (Not Recently Used)
    private Page remplacementNRU(List<Page> cadres) {
        // Classe 0: (R=0, M=0)
        Optional<Page> classe0 = cadres.stream()
            .filter(p -> !p.referenced && !p.modified)
            .findFirst();
        if (classe0.isPresent()) return classe0.get();

        // Classe 1: (R=0, M=1)
        Optional<Page> classe1 = cadres.stream()
            .filter(p -> !p.referenced && p.modified)
            .findFirst();
        if (classe1.isPresent()) return classe1.get();

        // Classe 2: (R=1, M=0)
        Optional<Page> classe2 = cadres.stream()
            .filter(p -> p.referenced && !p.modified)
            .findFirst();
        if (classe2.isPresent()) return classe2.get();

        // Classe 3: (R=1, M=1)
        return cadres.stream()
            .filter(p -> p.referenced && p.modified)
            .findFirst()
            .orElse(null);
    }

    // Implémentation de l'algorithme Seconde Chance
    private Page remplacementSecondeChance(List<Page> cadres) {
        while (true) {
            for (Page page : cadres) {
                if (!page.referenced) {
                    return page;
                }
                // Donner une seconde chance
                page.referenced = false;
            }
        }
    }

    // Implémentation de l'algorithme Optimal
    private Page remplacementOptimal(List<Page> cadres) {
        // Dans un cas réel, cet algorithme nécessiterait de connaître les futures références
        // Pour la simulation, nous pouvons utiliser une approche simplifiée
        return cadres.get(0); // Pour la simulation, on prend simplement la première page
    }

    // Mettre à jour les bits d'accès d'une page
    private void mettreAJourAcces(String processusId, int numeroPage) {
        List<Page> cadres = allocationLocale ? cadresParProcessus.get(processusId) : cadresGlobaux;
        if (cadres != null) {
            cadres.stream()
                .filter(p -> p.numeroPage == numeroPage && p.processusId.equals(processusId))
                .findFirst()
                .ifPresent(p -> {
                    p.referenced = true;
                    p.lastAccess = System.currentTimeMillis();
                });
        }
    }

    // Obtenir le nombre de défauts de page
    public int getDefautsDePage() {
        return defautsDePage;
    }

    // Obtenir la trace d'utilisation de la mémoire
    public String getTraceMemoire() {
        StringBuilder trace = new StringBuilder();
        trace.append("Trace d'utilisation de la mémoire:\n");
        trace.append("Nombre total de défauts de page: ").append(defautsDePage).append("\n");
        
        if (allocationLocale) {
            cadresParProcessus.forEach((processusId, cadres) -> {
                trace.append("Processus ").append(processusId).append(":\n");
                cadres.forEach(page -> 
                    trace.append("  Page ").append(page.numeroPage)
                         .append(" (R=").append(page.referenced ? "1" : "0")
                         .append(", M=").append(page.modified ? "1" : "0")
                         .append(")\n")
                );
            });
        } else {
            trace.append("Mémoire globale:\n");
            cadresGlobaux.forEach(page -> 
                trace.append("  Processus ").append(page.processusId)
                     .append(", Page ").append(page.numeroPage)
                     .append(" (R=").append(page.referenced ? "1" : "0")
                     .append(", M=").append(page.modified ? "1" : "0")
                     .append(")\n")
            );
        }
        
        return trace.toString();
    }
} 