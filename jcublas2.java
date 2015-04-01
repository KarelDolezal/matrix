/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jcublas;

import java.util.Random;


import jcuda.*;
import static jcuda.jcublas.JCublas2.cublasCreate;
import static jcuda.jcublas.JCublas2.cublasDestroy;
import static jcuda.jcublas.JCublas2.cublasGetVector;
import static jcuda.jcublas.JCublas2.cublasSetVector;
import static jcuda.jcublas.JCublas2.cublasSgemm;
import jcuda.jcublas.cublasHandle;
import static jcuda.jcublas.cublasOperation.CUBLAS_OP_N;
import static jcuda.runtime.JCuda.cudaFree;
import static jcuda.runtime.JCuda.cudaMalloc;

/**
 *
 * @author Kajo
 */
public class jcublas2 {
    public static void main(String args[])
    {
        testSgemm(1000,1500,1500,1000);
    }
    
    public static void testSgemm(int n1, int n2, int m1, int m2)
    {
        float alpha = 0.3f;
        float beta = 0.7f;
    int nn = n1 * n2;
    int mm = m1 * m2;
    
    System.out.println("Creating input data...");
        float h_A[] = createRandomFloatData(nn);
        float h_B[] = createRandomFloatData(mm);
        float h_C[] = createRandomFloatData(nn);
        float h_C_ref[] = h_C.clone();
        
        System.out.println("Performing Sgemm with Java...");
        long start = System.currentTimeMillis(); 
        sgemmJava(n1, n2, m1, m2, alpha, h_A, h_B, beta, h_C_ref);
        long end = System.currentTimeMillis() - start;
        System.out.println("Vysledny čas: "  +end);
        
        System.out.println("Performing Sgemm with JCublas...");
        sgemmJCublas(n1, n2, m1, m2, alpha, h_A, h_B, beta, h_C);
        
        boolean passed = isCorrectResult(h_C, h_C_ref);
        System.out.println("testSgemm "+(passed?"PASSED":"FAILED"));
        
        
        
    }
    
    private static void sgemmJCublas(int n1, int n2, int m1, int m2, float alpha, float A[], float B[],
                    float beta, float C[])
    {
        int nn = n1 * n2;
        int mm = m1 * m2;
        
        // Create a CUBLAS handle
        cublasHandle handle = new cublasHandle();
        cublasCreate(handle);
        cublasHandle handle2 = new cublasHandle();
        cublasCreate(handle2);
        cublasHandle handle3 = new cublasHandle();
        cublasCreate(handle3);
        cublasHandle handle4 = new cublasHandle();
        cublasCreate(handle4);
        
        // Allocate memory on the device
        Pointer d_A = new Pointer();
        Pointer d_B = new Pointer();
        Pointer d_C = new Pointer();
        cudaMalloc(d_A, nn * Sizeof.FLOAT);
        cudaMalloc(d_B, mm * Sizeof.FLOAT);
        cudaMalloc(d_C, nn * Sizeof.FLOAT);
        
        // Copy the memory from the host to the device
        cublasSetVector(nn, Sizeof.FLOAT, Pointer.to(A), 1, d_A, 1);
        cublasSetVector(mm, Sizeof.FLOAT, Pointer.to(B), 1, d_B, 1);
        cublasSetVector(nn, Sizeof.FLOAT, Pointer.to(C), 1, d_C, 1);
        
        // Execute sgemm
        Pointer pAlpha = Pointer.to(new float[]{alpha});
        Pointer pBeta = Pointer.to(new float[]{beta});
        cublasSgemm(handle, CUBLAS_OP_N, CUBLAS_OP_N, n1, n1, n1, 
            pAlpha, d_A, n1, d_B, n1, pBeta, d_C, n1);
        cublasSgemm(handle2, CUBLAS_OP_N, CUBLAS_OP_N, n2, n2, n2, 
            pAlpha, d_A, n2, d_B, n2, pBeta, d_C, n2);
        cublasSgemm(handle3, CUBLAS_OP_N, CUBLAS_OP_N, m1, m1, m1, 
            pAlpha, d_A, m1, d_B, m1, pBeta, d_C, m1);
        cublasSgemm(handle4, CUBLAS_OP_N, CUBLAS_OP_N, m2, m2, m2, 
            pAlpha, d_A, m2, d_B, m2, pBeta, d_C, m2);
        
        
        // Copy the result from the device to the host
        cublasGetVector(nn, Sizeof.FLOAT, d_C, 1, Pointer.to(C), 1);
        cublasGetVector(mm, Sizeof.FLOAT, d_C, 1, Pointer.to(C), 1);
        
        
        
        // Clean up
        cudaFree(d_A);
        cudaFree(d_B);
        cudaFree(d_C);
        cublasDestroy(handle);
        cublasDestroy(handle2);
        cublasDestroy(handle3);
        cublasDestroy(handle4);
    
    }
   
   
    private static void sgemmJava(int n1, int n2, int m1, int m2, float alpha, float A[], float B[],
                    float beta, float C[])
    {
        for (int i = 0; i < n1; ++i)
        {
            for (int j = 0; j < m2; ++j)
            {
                float prod = 0;
                for (int k = 0; k < n2; ++k)
                {
                    prod += A[k * n1 + i] * B[j * m1 + k];
                }
                C[j * n1 + i] = alpha * prod + beta * C[j * m1 + i];
            }
        }
    }
    
    
    
    private static float[] createRandomFloatData(int m)
    {
        Random random = new Random();
        float x[] = new float[m];
        for (int i = 0; i < m; i++)
        {
            x[i] = random.nextFloat();
        }
        return x;
    }
    private static boolean isCorrectResult(float result[], float reference[])
    {
        float errorNorm = 0;
        float refNorm = 0;
        for (int i = 0; i < result.length; ++i)
        {
            float diff = reference[i] - result[i];
            errorNorm += diff * diff;
            refNorm += reference[i] * result[i];
        }
        errorNorm = (float) Math.sqrt(errorNorm);
        refNorm = (float) Math.sqrt(refNorm);
        if (Math.abs(refNorm) < 1e-6)
        {
            return false;
        }
        return (errorNorm / refNorm < 1e-6f);
    }/////////////////
}
