/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package matrix;


import java.util.Random;
import java.util.Scanner;
/**
 *
 * @author Kajo
 */
class Matrix {

public static void printMatrix(double[][] m) {
    if (m == null) {
      System.out.println("null");
    } else {
      for (int i = 0; i < m.length; i++) {
        for (int j = 0; j < m[i].length; j++) {
          if (j == 0) {
            System.out.print("(");
          }
          System.out.print(m[i][j]);
          if (j != m[i].length - 1)
            System.out.print("\t ");
          else
            System.out.print(")");
        }
        System.out.println();
      }
    }
  }

  /**
   * Nasobi dve matice a vrati vysledek nebo null, 
   * pokud nasobeni nelze provest
   * 
   * @param m1 prvni matice pro nasobeni
   * @param m2 druha matice pro nasobeni
   * @return vysledna matice po vynasobeni, pokud nelze nasobit vraci null
   */
  public static double[][] multiplyMatrixs(double[][] m1, double[][] m2) {
    
    double[][] result;
    
    if ((m1.length == m2[0].length) && (m1[0].length == m2.length)) {
      
      result = new double[m1.length][m2[0].length];
      /* mozne nastaveni hodnot na nulu */
      
      /* implementace tri vnorenych cyklu pro nasobeni*/
      for (int i=0;i<m1.length;i++) {
        for (int j=0;j<m2[0].length;j++) {
          for (int k=0;k<m1[0].length;k++) {
            result[i][j] += m1[i][k]*m2[k][j];               
          }
        }        
      }
      
    } else {
      result = null;
    }

    return result;
  }

  /**
   * Startovni metoda programu
   * 
   * @param args vstupni parametry programu
   */
  public static void main(String[] args) {
    int n1 = 2000;
    int n2 = 1000;
    double [][] matice1 = new double [n1][n2];
    int m1 = 1000;
    int m2 = 2000; 
    double [][] matice2 = new double [m1][m2];
    
      
    Random generator = new Random ();
    for (int a=0;a<n1;a++) {
      for (int b=0;b<n2;b++) {
    matice1[a][b] = generator.nextInt();
     }
    }
    
    
    
    for (int a=0;a<m1;a++) {
      for (int b=0;b<m2;b++) {
    matice2[a][b] = generator.nextInt();
     }
    }
    //vypis matic
    //System.out.println("Prvni matice:");
    //printMatrix(matice1);
    //System.out.println("Druha matice:");
    //printMatrix(matice2);
    
    long start = System.currentTimeMillis();
    double[][] matice3 = multiplyMatrixs(matice1, matice2);
    //System.out.println("Vysledna matice po nasobeni:");
    //printMatrix(matice3);
    long end = System.currentTimeMillis() - start;
    System.out.println("vysledny čas: " +end);
  }
}
