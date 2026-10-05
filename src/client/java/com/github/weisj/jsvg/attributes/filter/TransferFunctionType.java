package com.github.weisj.jsvg.attributes.filter;

public enum TransferFunctionType {
   Identity,
   Table,
   Discrete,
   Linear,
   Gamma;

   // $VF: synthetic method
   private static TransferFunctionType[] $values() {
      return new TransferFunctionType[]{Identity, Table, Discrete, Linear, Gamma};
   }
}
