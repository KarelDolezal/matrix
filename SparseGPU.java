/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sparse;


import static jcuda.jcusparse.JCusparse.*;
import static jcuda.jcusparse.cusparseIndexBase.CUSPARSE_INDEX_BASE_ZERO;
import static jcuda.jcusparse.cusparseMatrixType.CUSPARSE_MATRIX_TYPE_GENERAL;
import static jcuda.jcusparse.cusparseOperation.CUSPARSE_OPERATION_NON_TRANSPOSE;
import static jcuda.runtime.JCuda.*;
import static jcuda.runtime.cudaMemcpyKind.*;
import jcuda.*;
import jcuda.jcusparse.*;
import jcuda.runtime.JCuda;

import java.util.Random;


/**
 * A sample application showing how to use JCusparse.<br />
 * <br />
 * This sample has been ported from the NVIDIA CUSPARSE
 * documentation example.
 */
public class SparseGPU
{
    public static void main(String args[])
    {
        // Enable exceptions and subsequently omit error checks in this sample
        JCusparse.setExceptionsEnabled(true);
        JCuda.setExceptionsEnabled(true);

        // Variable declarations
        cusparseHandle handle = new cusparseHandle();
        cusparseMatDescr descra = new cusparseMatDescr();

        int cooRowIndexHostPtr[];
        int cooColIndexHostPtr[];
        float cooValHostPtr[];
        int cooRowIndexHostPtrM[];
        int cooColIndexHostPtrM[];
        float cooValHostPtrM[];
        
        Pointer cooRowIndex = new Pointer();
        Pointer cooColIndex = new Pointer();
        Pointer cooVal = new Pointer();
        
        Pointer cooRowIndexM = new Pointer();
        Pointer cooColIndexM = new Pointer();
        Pointer cooValM = new Pointer();


        Pointer xInd = new Pointer();
        Pointer xVal = new Pointer();
        Pointer y = new Pointer();
        Pointer k = new Pointer();
        Pointer csrRowPtr = new Pointer();
        Pointer csrRowPtrM = new Pointer();
        
        
        Pointer z = new Pointer();
        int n, nnz, m, mmz, i, j;

        System.out.println("Testing example");

        // Create the following sparse test matrix in COO format
        Random generator = new Random();
        n = 100;
        nnz = 100;
        cooRowIndexHostPtr = new int[nnz];
        cooColIndexHostPtr = new int[nnz];
        cooValHostPtr      = new float[nnz];
        
        for (i=0; i<nnz;i++){
            int a = generator.nextInt(nnz);
            int b = generator.nextInt(nnz);
            if (a != cooRowIndexHostPtr[generator.nextInt(nnz-1)] & b != cooColIndexHostPtr[generator.nextInt(nnz-1)]){ 
            cooRowIndexHostPtr[generator.nextInt(nnz)]= a; 
            cooColIndexHostPtr[generator.nextInt(nnz)]= b; 
            cooValHostPtr[generator.nextInt(nnz)]=generator.nextInt(10); 
            }
            else
                cooRowIndexHostPtr[generator.nextInt(nnz)]= a; 
            cooColIndexHostPtr[generator.nextInt(nnz)]= b; 
            cooValHostPtr[generator.nextInt(nnz)]=generator.nextInt(10); 
        }
        
        
        m = 100;  
        mmz = 50;
        cooRowIndexHostPtrM = new int[mmz];
        cooColIndexHostPtrM = new int[mmz];
        cooValHostPtrM      = new float[mmz];
       
        for (j=0; j<mmz;j++){
            int a = generator.nextInt(mmz);
            int b = generator.nextInt(mmz);
            if (a != cooRowIndexHostPtrM[generator.nextInt(mmz-1)] & b != cooColIndexHostPtrM[generator.nextInt(mmz-1)]){ 
            cooRowIndexHostPtrM[generator.nextInt(mmz)]= a; 
            cooColIndexHostPtrM[generator.nextInt(mmz)]= b; 
            cooValHostPtrM[generator.nextInt(mmz)]=generator.nextInt(10); 
            }
            else
                cooRowIndexHostPtrM[generator.nextInt(mmz)]= a; 
            cooColIndexHostPtrM[generator.nextInt(mmz)]= b; 
            cooValHostPtrM[generator.nextInt(mmz)]=generator.nextInt(10); 
        }
            
        // Print the matrix
        System.out.printf("Input data matrix 1:\n");
        for (i=0; i<nnz; i++)
        {
            System.out.printf("cooRowIndedHostPtr[%d]=%d  ",i,cooRowIndexHostPtr[i]);
            System.out.printf("cooColIndedHostPtr[%d]=%d  ",i,cooColIndexHostPtr[i]);
            System.out.printf("cooValHostPtr[%d]=%f     \n",i,cooValHostPtr[i]);
        }
        
        
        System.out.printf("Input data matrix 2:\n");
        for (i=0; i<mmz; i++)
        {
            System.out.printf("cooRowIndedHostPtrM[%d]=%d  ",i,cooRowIndexHostPtrM[i]);
            System.out.printf("cooColIndedHostPtrM[%d]=%d  ",i,cooColIndexHostPtrM[i]);
            System.out.printf("cooValHostPtrM[%d]=%f     \n",i,cooValHostPtrM[i]);
        }
        
        // Allocate GPU memory and copy the matrix and vectors into it
        cudaMalloc(cooRowIndex, nnz*Sizeof.INT);
        cudaMalloc(cooColIndex, nnz*Sizeof.INT);
        cudaMalloc(cooVal,      nnz*Sizeof.FLOAT);
        cudaMalloc(y,           2*n*Sizeof.FLOAT);
        cudaMalloc(cooRowIndexM, mmz*Sizeof.INT);
        cudaMalloc(cooColIndexM, mmz*Sizeof.INT);
        cudaMalloc(cooValM,      mmz*Sizeof.FLOAT);
        cudaMalloc(k,           2*m*Sizeof.FLOAT);
        
       
        cudaMemcpy(cooRowIndex, Pointer.to(cooRowIndexHostPtr), nnz*Sizeof.INT,          cudaMemcpyHostToDevice);
        cudaMemcpy(cooColIndex, Pointer.to(cooColIndexHostPtr), nnz*Sizeof.INT,          cudaMemcpyHostToDevice);
        cudaMemcpy(cooVal,      Pointer.to(cooValHostPtr),      nnz*Sizeof.FLOAT,        cudaMemcpyHostToDevice);
        cudaMemcpy(cooRowIndexM, Pointer.to(cooRowIndexHostPtrM), mmz*Sizeof.INT,          cudaMemcpyHostToDevice);
        cudaMemcpy(cooColIndexM, Pointer.to(cooColIndexHostPtrM), mmz*Sizeof.INT,          cudaMemcpyHostToDevice);
        cudaMemcpy(cooValM,      Pointer.to(cooValHostPtrM),      mmz*Sizeof.FLOAT,        cudaMemcpyHostToDevice);
                

        // Initialize JCusparse library
        cusparseCreate(handle);

        // Create and set up matrix descriptor
        cusparseCreateMatDescr(descra);
        cusparseSetMatType(descra, CUSPARSE_MATRIX_TYPE_GENERAL);
        cusparseSetMatIndexBase(descra, CUSPARSE_INDEX_BASE_ZERO);

        // Exercise conversion routines (convert matrix from COO 2 CSR format)
        cudaMalloc(csrRowPtr, (n+1)*Sizeof.INT);
        cusparseXcoo2csr(handle, cooRowIndex, nnz, n,
            csrRowPtr, CUSPARSE_INDEX_BASE_ZERO);
        
        
        cudaMalloc(csrRowPtrM, (m+1)*Sizeof.INT);
        cusparseXcoo2csr(handle, cooRowIndexM, mmz, m,
            csrRowPtrM, CUSPARSE_INDEX_BASE_ZERO);        
      
        // Exercise Level 3 routines (csrmm)
       cudaMalloc(z, 2*(n+1)*Sizeof.FLOAT);
       cudaMalloc(z, 2*(m+1)*Sizeof.FLOAT);
        cudaMemset(z, 0, 2*(n+1)*Sizeof.FLOAT);
        cudaMemset(z, 0, 2*(m+1)*Sizeof.FLOAT);
        cusparseScsrmm(handle, CUSPARSE_OPERATION_NON_TRANSPOSE, n, 2, n, nnz,
            Pointer.to(new float[]{5.0f}), descra, cooVal, csrRowPtr, 
            cooColIndex, y, n, Pointer.to(new float[]{0.0f}), z, n+1); 
        cusparseScsrmm(handle, CUSPARSE_OPERATION_NON_TRANSPOSE, m, 2, m, mmz,
            Pointer.to(new float[]{5.0f}), descra, cooValM, csrRowPtrM, 
            cooColIndexM, k, m, Pointer.to(new float[]{0.0f}), z, m+1);        

        // Clean up
        cudaFree(y);
        cudaFree(k);
        cudaFree(z);
        cudaFree(xInd);
        cudaFree(xVal);
        cudaFree(csrRowPtr);
        cudaFree(csrRowPtrM);
        cudaFree(cooRowIndex);
        cudaFree(cooColIndex);
        cudaFree(cooVal);
        cudaFree(cooRowIndexM);
        cudaFree(cooColIndexM);
        cudaFree(cooValM);
        cusparseDestroy(handle);
    }
}


