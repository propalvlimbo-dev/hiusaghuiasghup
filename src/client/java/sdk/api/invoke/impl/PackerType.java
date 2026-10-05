package sdk.api.invoke.impl;

public enum PackerType {
   NONE,
   MUTATION,
   VIRTUALIZATION,
   ULTRA;

   // $VF: synthetic method
   private static PackerType[] $values() {
      return new PackerType[]{NONE, MUTATION, VIRTUALIZATION, ULTRA};
   }
}
