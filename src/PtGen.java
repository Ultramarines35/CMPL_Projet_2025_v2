
/*********************************************************************************
 * VARIABLES ET METHODES FOURNIES PAR LA CLASSE UtilLex (cf libClass_Projet)     *
 *       complement à l'ANALYSEUR LEXICAL produit par ANTLR                      *
 *                                                                               *
 *                                                                               *
 *   nom du programme compile, sans suffixe : String UtilLex.nomSource           *
 *   ------------------------                                                    *
 *                                                                               *
 *   attributs lexicaux (selon items figurant dans la grammaire):                *
 *   ------------------                                                          *
 *     int UtilLex.valEnt = valeur du dernier nombre entier lu (item nbentier)   *
 *     int UtilLex.numIdCourant = code du dernier identificateur lu (item ident) *
 *                                                                               *
 *                                                                               *
 *   methodes utiles :                                                           *
 *   ---------------                                                             *
 *     void UtilLex.messErr(String m)  affichage de m et arret compilation       *
 *     String UtilLex.chaineIdent(int numId) delivre l'ident de codage numId     *
 *     void afftabSymb()  affiche la table des symboles                          *
 *********************************************************************************/

import java.io.*;

/**
 * classe de mise en oeuvre du compilateur
 * =======================================
 * (verifications semantiques + production du code objet)
 * 
 * @author Girard, Masson, Perraudeau
 *
 */

public class PtGen {

	// constantes manipulees par le compilateur
	// ----------------------------------------

	private static final int

	// taille max de la table des symboles
	MAXSYMB = 300,

			// codes MAPILE :
			RESERVER = 1, EMPILER = 2, CONTENUG = 3, AFFECTERG = 4, OU = 5, ET = 6, NON = 7, INF = 8,
			INFEG = 9, SUP = 10, SUPEG = 11, EG = 12, DIFF = 13, ADD = 14, SOUS = 15, MUL = 16, DIV = 17,
			BSIFAUX = 18, BINCOND = 19, LIRENT = 20, LIREBOOL = 21, ECRENT = 22, ECRBOOL = 23,
			ARRET = 24, EMPILERADG = 25, EMPILERADL = 26, CONTENUL = 27, AFFECTERL = 28,
			APPEL = 29, RETOUR = 30,

			// codes des valeurs vrai/faux
			VRAI = 1, FAUX = 0,

			// types permis :
			ENT = 1, BOOL = 2, NEUTRE = 3,

			// categories possibles des identificateurs :
			CONSTANTE = 1, VARGLOBALE = 2, VARLOCALE = 3, PARAMFIXE = 4, PARAMMOD = 5, PROC = 6,
			DEF = 7, REF = 8, PRIVEE = 9,

			// valeurs possible du vecteur de translation
			TRANSDON = 1, TRANSCODE = 2, REFEXT = 3;

	// utilitaires de controle de type
	// -------------------------------
	/**
	 * verification du type entier de l'expression en cours de compilation
	 * (arret de la compilation sinon)
	 */
	private static void verifEnt() {
		if (tCour != ENT)
			UtilLex.messErr("expression entiere attendue");
	}

	/**
	 * verification du type booleen de l'expression en cours de compilation
	 * (arret de la compilation sinon)
	 */
	private static void verifBool() {
		if (tCour != BOOL)
			UtilLex.messErr("expression booleenne attendue");
	}

	// pile pour gerer les chaines de reprise et les branchements en avant
	// -------------------------------------------------------------------

	private static TPileRep pileRep;

	// production du code objet en memoire
	// -----------------------------------

	private static ProgObjet po;

	// COMPILATION SEPAREE
	// -------------------
	//
	/**
	 * modification du vecteur de translation associe au code produit
	 * + incrementation attribut nbTransExt du descripteur
	 * NB: effectue uniquement si c'est une reference externe ou si on compile un
	 * module
	 * 
	 * @param valeur : TRANSDON, TRANSCODE ou REFEXT
	 */
	private static void modifVecteurTrans(int valeur) {
		if (valeur == REFEXT || desc.getUnite().equals("module")) {
			po.vecteurTrans(valeur);
			desc.incrNbTansExt();
		}
	}

	// descripteur associe a un programme objet (compilation separee)
	private static Descripteur desc;

	// autres variables fournies
	// -------------------------

	// MERCI de renseigner ici un nom pour le trinome, constitue EXCLUSIVEMENT DE
	// LETTRES
	public static String trinome = "DELAPART Jules RECIPON Pierre"; // TODO

	private static int tCour; // type de l'expression compilee
	private static int vCour; // sert uniquement lors de la compilation d'une valeur (entiere ou boolenne)

	// TABLE DES SYMBOLES
	// ------------------
	//
	private static EltTabSymb[] tabSymb = new EltTabSymb[MAXSYMB + 1];

	// it = indice de remplissage de tabSymb
	// bc = bloc courant (=1 si le bloc courant est le programme principal)
	private static int it, bc;

	/**
	 * utilitaire de recherche de l'ident courant (ayant pour code
	 * UtilLex.numIdCourant) dans tabSymb
	 * 
	 * @param borneInf : recherche de l'indice it vers borneInf (=1 si recherche
	 *                 dans tout tabSymb)
	 * @return : indice de l'ident courant (de code UtilLex.numIdCourant) dans
	 *         tabSymb (O si absence)
	 */
	private static int presentIdent(int borneInf) {
		int i = it;
		while (i >= borneInf && tabSymb[i].code != UtilLex.numIdCourant)
			i--;
		if (i >= borneInf)
			return i;
		else
			return 0;
	}

	/**
	 * utilitaire de placement des caracteristiques d'un nouvel ident dans tabSymb
	 * 
	 * @param code : UtilLex.numIdCourant de l'ident
	 * @param cat  : categorie de l'ident parmi CONSTANTE, VARGLOBALE, PROC, etc.
	 * @param type : ENT, BOOL ou NEUTRE
	 * @param info : valeur pour une constante, ad d'exécution pour une variable,
	 *             etc.
	 */
	private static void placeIdent(int code, int cat, int type, int info) {
		if (it == MAXSYMB)
			UtilLex.messErr("debordement de la table des symboles");
		it = it + 1;
		tabSymb[it] = new EltTabSymb(code, cat, type, info);
	}

	/**
	 * utilitaire d'affichage de la table des symboles
	 */
	private static void afftabSymb() {
		System.out.println("       code           categorie      type    info");
		System.out.println("      |--------------|--------------|-------|----");
		for (int i = 1; i <= it; i++) {
			if (i == bc) {
				System.out.print("bc=");
				Ecriture.ecrireInt(i, 3);
			} else if (i == it) {
				System.out.print("it=");
				Ecriture.ecrireInt(i, 3);
			} else
				Ecriture.ecrireInt(i, 6);
			if (tabSymb[i] == null)
				System.out.println(" reference NULL");
			else
				System.out.println(" " + tabSymb[i]);
		}
	}

	static int compteurVar;
	static int compteurVarLoc;
	static int compteurConstLoc;
	static int compteurPara;
	static int idConst;
	static int tConst;
	static int ident_tmp;
	static int affect_ident_tmp;
	static int val_tmp;
	static boolean reserver;
	static int tmp_boucle;

	/**
	 * initialisations A COMPLETER SI BESOIN
	 * -------------------------------------
	 */
	public static void initialisations() {

		// indices de gestion de la table des symboles
		it = 0;
		bc = 1;
		// pile des reprises pour compilation des branchements en avant
		pileRep = new TPileRep();
		// programme objet = code Mapile de l'unite en cours de compilation
		po = new ProgObjet();
		// COMPILATION SEPAREE: desripteur de l'unite en cours de compilation
		desc = new Descripteur();

		// initialisation necessaire aux attributs lexicaux
		UtilLex.initialisation();

		// initialisation du type de l'expression courante
		tCour = NEUTRE;
		tConst = NEUTRE;
		// TODO si necessaire
		compteurVar = 0;
		compteurPara = 0;
		compteurVarLoc = 0;
		reserver = false;

	} // initialisations

	/**
	 * code des points de generation A COMPLETER
	 * -----------------------------------------
	 * 
	 * @param numGen : numero du point de generation a executer
	 */
	public static void pt(int numGen) {
		System.out.println(numGen);
		switch (numGen) {
			case 0:
				initialisations();
				break;

			// TODO

			case 1: // Verifier que l element dans la pile est un booleen
				verifBool();
				break;

			case 2: // verifier que l element dans la pile est un entier
				verifEnt();
				break;

			case 3: // Modification de type : ENT

				tCour = ENT;
				break;

			case 4: // Modification de type : BOOL
				tCour = BOOL;
				break;

			case 5: // Ajout TabSymb VARGLOB/VARLOC
				if (presentIdent(bc) != 0) {
					UtilLex.messErr("Erreur : Double déclaration de variable");
				} else {
					int tmp_ident = UtilLex.numIdCourant;
					if (bc == 1) {

						placeIdent(tmp_ident, VARGLOBALE, tCour, compteurVar);
						compteurVar++;
						afftabSymb();
					} else {
						placeIdent(tmp_ident, VARLOCALE, tCour, compteurVarLoc);
						compteurVarLoc++;
						afftabSymb();
					}
				}
				break;

			case 6: // Actualisation Ident CONST
				// idConst = UtilLex.numIdCourant;
				break;

			case 7: // Ajout TabSymb CONST
				if (presentIdent(bc) == 0) {
					placeIdent(UtilLex.numIdCourant, CONSTANTE, tCour, vCour);
					if(bc != 1){
					}
				} else {
					UtilLex.messErr("Erreur : Double déclaration de Constante");
				}
				break;

			case 8: // lecture d'une valeur entière positive ou une valeur booléene
				vCour = UtilLex.valEnt;
				break;

			case 9: // lecture d'un entier négatif
				vCour = UtilLex.valEnt * (-1);
				break;

			case 10: // Reservation des Variables globales
				po.produire(RESERVER);
				po.produire(compteurVar);
				break;

			case 11: // lecture d'un ident
				ident_tmp = presentIdent(bc);
				if (ident_tmp != 0) {
					//1er partie, on génère de quoi mettre l'ident dans la pile : contenug/contenul/empiler
					int tmp = tabSymb[ident_tmp].categorie;
					if (tmp == VARGLOBALE) {
						po.produire(CONTENUG);
						po.produire(tabSymb[ident_tmp].info);
					}
					else if (tmp == VARLOCALE || tmp == PARAMFIXE) {
						po.produire(CONTENUL);
						po.produire(tabSymb[ident_tmp].info);
						po.produire(0);
					}
					else if (tmp == PARAMMOD) {
						po.produire(CONTENUL);
						po.produire(tabSymb[ident_tmp].info);
						po.produire(1);
					}
					else if (tmp == CONSTANTE) {
						po.produire(EMPILER);
						po.produire(tabSymb[ident_tmp].info);
					}
					else {
						UtilLex.messErr("Erreur de type de Ident : " + tabSymb[ident_tmp].categorie);
					}
					if (tabSymb[ident_tmp].type == ENT) {
						tCour = ENT;
					} else if (tabSymb[ident_tmp].type == BOOL) {
						tCour = BOOL;
					} else {
						UtilLex.messErr("Erreur de type de Ident : Type interdit");
					}
					ident_tmp = tabSymb[ident_tmp].info;
				} else {
					UtilLex.messErr("Erreur de type de Ident : Ident inconnu");
				}
				break;

			case 12: // Production lirent/lirebool (lire)
				if (tCour == BOOL) {
					po.produire(ECRBOOL);
				} else if (tCour == ENT) {
					po.produire(ECRENT);

				} else {
					UtilLex.messErr("Erreur de type de Ident : Ident inconnu");
				}
				break;

			case 13: // Empiler ident pour l'affectation
				// checker si pas constante et ils sont de meme type
				afftabSymb();
				affect_ident_tmp = presentIdent(bc);
				if (affect_ident_tmp != 0) {
					//affect_ident_tmp = UtilLex.numIdCourant;
				} else {
					UtilLex.messErr("Erreur : AFFOUAPPEL, ident n'est pas dans la table");
				}
				break;

			case 14:
				if(tabSymb[affect_ident_tmp].type != tCour){
					UtilLex.messErr(tCour + " attendu");
				}
				else{
					if(bc > 1){
						if(tabSymb[presentIdent(1)].categorie == VARGLOBALE){
							po.produire(AFFECTERG);
							po.produire(tabSymb[affect_ident_tmp].info);
						}
						else if (tabSymb[presentIdent(bc)].categorie == VARLOCALE){
							po.produire(AFFECTERL);
							po.produire(tabSymb[affect_ident_tmp].info);
							po.produire(0);
						}
						else if (tabSymb[presentIdent(bc)].categorie == CONSTANTE){
							System.out.println("CEST LA ");
						}
						else if (tabSymb[presentIdent(bc)].categorie == PARAMMOD){
							po.produire(AFFECTERL);
							po.produire(tabSymb[affect_ident_tmp].info);
							po.produire(1);
						}
						else if (tabSymb[presentIdent(1)].categorie == PROC) {
							System.out.println("mettre un int ici pour recuperer l'indice de la PROC");
						}
						else {
							UtilLex.messErr("Erreur : AFFOUAPPEL, ident n'est ni une varlocale, ni une varglobale ni un paramod");
						}
					}
					else{
						if(tabSymb[presentIdent(1)].categorie == VARGLOBALE){
							po.produire(AFFECTERG);
							po.produire(tabSymb[affect_ident_tmp].info);
						}
						else{
							UtilLex.messErr("Erreur : AFFOUAPPEL, ident n'est pas une varglobale");
						}
					}
				}
				break;

			case 15:
				po.produire(EG);
				tCour = BOOL;
				break;

			case 16:
				po.produire(DIFF);
				tCour = BOOL;
				break;

			case 17:
				po.produire(SUP);
				tCour = BOOL;
				break;

			case 18:
				po.produire(SUPEG);
				tCour = BOOL;
				break;

			case 19:
				po.produire(INF);
				tCour = BOOL;
				break;

			case 20:
				po.produire(INFEG);
				tCour = BOOL;
				break;

			case 21:
				reserver = true;
				break;

			case 22:
				po.produire(ADD);
				tCour = ENT;
				break;

			case 23:
				po.produire(SOUS);
				tCour = ENT;
				break;

			case 24:
				po.produire(MUL);
				tCour = ENT;
				break;

			case 25:
				verifEnt();
				break;

			case 26:
				po.produire(DIV);
				tCour = ENT;
				break;

			case 27:
				po.produire(OU);
				tCour = BOOL;
				break;

			case 28:
				po.produire(ET);
				tCour = BOOL;
				break;

			case 29:
				po.produire(NON);
				tCour = BOOL;
				break;

			case 30:
				ident_tmp = presentIdent(bc);
				if (ident_tmp != 0) {
					if (tabSymb[ident_tmp].categorie == VARGLOBALE) { // Nous devons differencier entre lirent et
																		// lirebool
						if (tabSymb[ident_tmp].type == ENT) {
							po.produire(LIRENT);
							po.produire(AFFECTERG);
							po.produire(tabSymb[ident_tmp].info);
						} else if (tabSymb[ident_tmp].type == BOOL) {
							po.produire(LIREBOOL);
							po.produire(AFFECTERG);
							po.produire(tabSymb[ident_tmp].info);
						} else
							UtilLex.messErr("Erreur : Type de donnée lu inconnu");

					} else {
						UtilLex.messErr("Erreur : (lire) lecture de CONST impossible");
					}

					tCour = tabSymb[ident_tmp].type;
				} else {
					UtilLex.messErr("Erreur de type de Ident : Ident inconnu");
				}



				tCour = tabSymb[ident_tmp].type;
				if(tCour == ENT) {
					po.produire(LIRENT);
				} else if(tCour == BOOL) {
					po.produire(LIREBOOL);
				}
				else{
					UtilLex.messErr("Erreur : Type neutre détecté");
				}

				if(tabSymb[ident_tmp].categorie == VARLOCALE) {
					po.produire(AFFECTERL);
					po.produire(tabSymb[ident_tmp].info);
					po.produire(0);
				} else if(tabSymb[ident_tmp].categorie == PARAMMOD) {
					po.produire(AFFECTERL);
					po.produire(tabSymb[ident_tmp].info);
					po.produire(1);
				} else if (tabSymb[ident_tmp].categorie == VARGLOBALE){
					po.produire(AFFECTERG);
					po.produire(tabSymb[ident_tmp].info);
				}
				else {
					UtilLex.messErr("Erreur : (lire) lecture du type impossible");
				}
				break;

			case 31: // Début Si : mettre bsifaux + empiler pile rep
				po.produire(BSIFAUX);
				po.produire(0);
				pileRep.empiler(po.getIpo());
				break;

			case 32: // Avant instr sinon
				po.produire(BINCOND);
				po.produire(0);
				int tmp = po.getIpo();
				po.modifier(pileRep.depiler(), po.getIpo() + 1);
				pileRep.empiler(tmp);
				break;

			case 33: // Résout bincond si y'a alors, sinon résout bsifaux
				po.modifier(pileRep.depiler(), po.getIpo() + 1);
				break;

			case 34:
				po.produire(BSIFAUX);
				po.produire(0);
				pileRep.empiler(po.getIpo());

				break;

			case 35:
				tmp_boucle = pileRep.depiler();
				po.produire(BINCOND);
				po.produire(tmp_boucle - 1);
				po.modifier(tmp_boucle, po.getIpo() + 1);
				break;

			// 36 - 39 COND
			case 36:
				pileRep.empiler(0);
				break;

			case 37:// Cas BINCOND, on relie le nouveau BINCOND à l'adresse de l'ancien BINCOND
				po.produire(BINCOND);
				po.modifier(pileRep.depiler(), po.getIpo() + 2);
				po.produire(pileRep.depiler());
				pileRep.empiler(po.getIpo());
				break;

			case 38:
				po.modifier(pileRep.depiler(), po.getIpo() + 1);
				break;

			case 39:
				int ad_temp = pileRep.depiler();
				while (ad_temp != 0) {
					int temp = po.getElt(ad_temp);
					po.modifier(ad_temp, po.getIpo() + 1);
					ad_temp = temp;
				}
				break;

			case 41:
				vCour = UtilLex.valEnt;
				po.produire(EMPILER);
				po.produire(vCour);
				break;

			case 42 ://Début proc
				placeIdent(UtilLex.numIdCourant, PROC, NEUTRE, 0);
				placeIdent(-1, PRIVEE, NEUTRE, 0);
				bc = it + 1;
				break;


				case 43: // decproc : parfixe
				placeIdent(UtilLex.numIdCourant, PARAMFIXE, tCour, compteurPara);
				compteurPara++;
				break;


			case 44: // decproc : parmod
				placeIdent(UtilLex.numIdCourant, PARAMMOD, tCour, compteurPara);
				compteurPara++;
				break;

			case 45: // decproc fin (Retour + Nettoyage de table)
				compteurVarLoc -= 2;
				compteurVarLoc += compteurConstLoc; //Pour enlever les constantes déclarer localement de la table
				po.produire(RETOUR);
				po.produire(compteurPara);
				for(int i = compteurVarLoc; i>0;i-- ){
					tabSymb[bc].code = -1;
					bc++;
				}
				compteurVarLoc -=compteurPara;
				it -= compteurVarLoc;
				compteurVarLoc = 0;
				bc = 1;
				afftabSymb();
				break;

			case 46: // Modif proc tab Symb (Ajout des paramètres)
				//tabSymb[bc - 1].info = compteurPara;
				compteurVarLoc = compteurPara + 2;
				break;

			case 47: //Reservation des varLocales
				po.produire(RESERVER);
				po.produire(compteurPara);
				break;


			case 48:// Modification bincond pour sauter les procs
			//po.modifier(pileRep.depiler(), po.getIpo()+1);
			break;

			case 49: // Appel des
			int tmp_presentIdent = presentIdent(bc);
				if (tmp_presentIdent != 0){
					if(tabSymb[tmp_presentIdent].categorie == VARGLOBALE){
						po.produire(EMPILERADG);
						po.produire(tmp_presentIdent);
					}
					else if (tabSymb[tmp_presentIdent].categorie == VARLOCALE){
						po.produire(EMPILERADL);
						po.produire(tmp_presentIdent);
						po.produire(0);
					}
					else if (tabSymb[tmp_presentIdent].categorie == PARAMMOD){
						po.produire(EMPILERADL);
						po.produire(tmp_presentIdent);
						po.produire(1);
					}
					else{
						UtilLex.messErr("Erreur de type de Ident : Passage en Effixes Invalide");
					}
				}
				else{
					UtilLex.messErr("Erreur de type de Ident : Passage en Effixes impossible, Ident inconnu");
				}
				break;

			case 50:	//correction de faux
				tCour = BOOL;
				vCour = 0;
				break;
			
			case 51:	//correction de vrai
				tCour = BOOL;
				vCour = 1;
				break;

			case 254:
				po.produire(ARRET);
				break;

			case 255:
				afftabSymb(); // affichage de la table des symboles en fin de compilation
				po.constObj();
				po.constGen();

				break;

			default:
				System.out.println("Point de generation non prevu dans votre liste");
				break;

		}
	}
}
