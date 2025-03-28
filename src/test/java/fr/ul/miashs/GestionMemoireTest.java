package fr.ul.miashs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GestionMemoireTest {
    private GestionMemoire gestionMemoireLocale;
    private GestionMemoire gestionMemoireGlobale;
    private static final int NOMBRE_CADRES_TOTAL = 8;
    private static final int NOMBRE_CADRES_LOCAL = 4;
    private static final int TEMPS_CHARGE_PAGE = 50;

    @BeforeEach
    void setUp() {
        // Initialisation avec allocation locale et algorithme FIFO
        gestionMemoireLocale = new GestionMemoire(
            NOMBRE_CADRES_TOTAL,
            NOMBRE_CADRES_LOCAL,
            true,  // allocation locale
            "FIFO",
            TEMPS_CHARGE_PAGE
        );

        // Initialisation avec allocation globale et algorithme FIFO
        gestionMemoireGlobale = new GestionMemoire(
            NOMBRE_CADRES_TOTAL,
            NOMBRE_CADRES_LOCAL,
            false,  // allocation globale
            "FIFO",
            TEMPS_CHARGE_PAGE
        );
    }

    @Test
    void testAllocationLocale() {
        // Test d'allocation pour un processus
        assertTrue(gestionMemoireLocale.accederPage("P1", 1));
        assertTrue(gestionMemoireLocale.accederPage("P1", 2));
        assertTrue(gestionMemoireLocale.accederPage("P1", 3));
        assertTrue(gestionMemoireLocale.accederPage("P1", 4));
        
        // Vérifier que le nombre de défauts de page est correct
        assertEquals(4, gestionMemoireLocale.getDefautsDePage());

        // Test de remplacement FIFO
        assertTrue(gestionMemoireLocale.accederPage("P1", 5));
        assertEquals(5, gestionMemoireLocale.getDefautsDePage());
    }

    @Test
    void testAllocationGlobale() {
        // Test d'allocation pour plusieurs processus
        assertTrue(gestionMemoireGlobale.accederPage("P1", 1));
        assertTrue(gestionMemoireGlobale.accederPage("P2", 1));
        assertTrue(gestionMemoireGlobale.accederPage("P3", 1));
        assertTrue(gestionMemoireGlobale.accederPage("P4", 1));
        
        assertEquals(4, gestionMemoireGlobale.getDefautsDePage());

        // Test de remplacement FIFO en global
        assertTrue(gestionMemoireGlobale.accederPage("P5", 1));
        assertTrue(gestionMemoireGlobale.accederPage("P6", 1));
        assertTrue(gestionMemoireGlobale.accederPage("P7", 1));
        assertTrue(gestionMemoireGlobale.accederPage("P8", 1));
        
        assertEquals(8, gestionMemoireGlobale.getDefautsDePage());
    }

    @Test
    void testAccesRepete() {
        // Test d'accès répété à la même page
        assertTrue(gestionMemoireLocale.accederPage("P1", 1));
        assertTrue(gestionMemoireLocale.accederPage("P1", 1));
        assertTrue(gestionMemoireLocale.accederPage("P1", 1));
        
        // Un seul défaut de page doit être compté
        assertEquals(1, gestionMemoireLocale.getDefautsDePage());
    }

    @Test
    void testAlgorithmeNRU() {
        GestionMemoire gestionMemoire = new GestionMemoire(
            NOMBRE_CADRES_TOTAL,
            NOMBRE_CADRES_LOCAL,
            true,
            "NRU",
            TEMPS_CHARGE_PAGE
        );

        // Remplir les cadres
        for (int i = 1; i <= 4; i++) {
            assertTrue(gestionMemoire.accederPage("P1", i));
        }

        // Accéder à nouveau à certaines pages pour mettre à jour leurs bits R
        assertTrue(gestionMemoire.accederPage("P1", 1));
        assertTrue(gestionMemoire.accederPage("P1", 2));

        // Forcer un remplacement
        assertTrue(gestionMemoire.accederPage("P1", 5));
        
        assertEquals(5, gestionMemoire.getDefautsDePage());
    }

    @Test
    void testAlgorithmeSecondeChance() {
        GestionMemoire gestionMemoire = new GestionMemoire(
            NOMBRE_CADRES_TOTAL,
            NOMBRE_CADRES_LOCAL,
            true,
            "SECONDE_CHANCE",
            TEMPS_CHARGE_PAGE
        );

        // Remplir les cadres
        for (int i = 1; i <= 4; i++) {
            assertTrue(gestionMemoire.accederPage("P1", i));
        }

        // Donner une seconde chance à certaines pages
        assertTrue(gestionMemoire.accederPage("P1", 1));
        assertTrue(gestionMemoire.accederPage("P1", 2));

        // Forcer un remplacement
        assertTrue(gestionMemoire.accederPage("P1", 5));
        
        assertEquals(5, gestionMemoire.getDefautsDePage());
    }

    @Test
    void testAlgorithmeOptimal() {
        GestionMemoire gestionMemoire = new GestionMemoire(
            NOMBRE_CADRES_TOTAL,
            NOMBRE_CADRES_LOCAL,
            true,
            "OPTIMAL",
            TEMPS_CHARGE_PAGE
        );

        // Test basique de l'algorithme optimal
        assertTrue(gestionMemoire.accederPage("P1", 1));
        assertTrue(gestionMemoire.accederPage("P1", 2));
        assertTrue(gestionMemoire.accederPage("P1", 3));
        assertTrue(gestionMemoire.accederPage("P1", 4));
        assertTrue(gestionMemoire.accederPage("P1", 5));

        assertEquals(5, gestionMemoire.getDefautsDePage());
    }

    @Test
    void testLimitesCadres() {
        // Test avec un processus qui tente d'utiliser plus que sa limite locale
        for (int i = 1; i <= NOMBRE_CADRES_LOCAL + 2; i++) {
            assertTrue(gestionMemoireLocale.accederPage("P1", i));
        }

        // Vérifier que le nombre de défauts de page est correct
        assertEquals(NOMBRE_CADRES_LOCAL + 2, gestionMemoireLocale.getDefautsDePage());
    }

    @Test
    void testTraceMemoire() {
        // Effectuer quelques accès mémoire
        gestionMemoireLocale.accederPage("P1", 1);
        gestionMemoireLocale.accederPage("P1", 2);
        gestionMemoireLocale.accederPage("P2", 1);

        // Vérifier que la trace contient les informations attendues
        String trace = gestionMemoireLocale.getTraceMemoire();
        assertTrue(trace.contains("Trace d'utilisation de la mémoire"));
        assertTrue(trace.contains("Nombre total de défauts de page: 3"));
        assertTrue(trace.contains("Processus P1"));
        assertTrue(trace.contains("Processus P2"));
    }
} 