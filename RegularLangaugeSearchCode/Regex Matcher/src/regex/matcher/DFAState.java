package regex.matcher;

import java.util.Set;

/**
 * DFA state containing references to corresponding NFA states
 *
 */
public class DFAState extends State {
        
        private Set<State> nfaStates;
        
        
        public DFAState() {}
        
        public DFAState(Set<State> nfaStates) {
                this.nfaStates = nfaStates;
        }

        
        public Set<State> getNfaStates() {
                return nfaStates;
        }

        public void setNfaStates(Set<State> nfaStates) {
                this.nfaStates = nfaStates;
        }


        @Override
        public String toString() {
                return new StringBuilder(super.toString()).
                        append(", NFA states: ").append(nfaStates).
                        toString();
        }

}